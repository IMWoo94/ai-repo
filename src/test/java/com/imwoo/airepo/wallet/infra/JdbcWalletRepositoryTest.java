package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.application.IdempotencyKeyConflictException;
import com.imwoo.airepo.wallet.application.InMemoryWalletCommandService;
import com.imwoo.airepo.wallet.application.InMemoryWalletLedgerQueryService;
import com.imwoo.airepo.wallet.application.InvalidWalletOperationException;
import com.imwoo.airepo.wallet.application.WalletChargeCommand;
import com.imwoo.airepo.wallet.application.WalletCommandResult;
import com.imwoo.airepo.wallet.application.WalletTransferCommand;
import com.imwoo.airepo.wallet.domain.AdminApiAccessAudit;
import com.imwoo.airepo.wallet.domain.AdminApiAccessOutcome;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueRequestStatus;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRun;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRunStatus;
import com.imwoo.airepo.wallet.domain.OperationOutboxStatus;
import com.imwoo.airepo.wallet.domain.OperationStep;
import com.imwoo.airepo.wallet.domain.TransactionDirection;
import com.imwoo.airepo.wallet.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcWalletRepositoryTest {

    private EmbeddedDatabase database;
    private JdbcWalletRepository repository;
    private InMemoryWalletCommandService commandService;
    private InMemoryWalletLedgerQueryService ledgerQueryService;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .setName("wallet;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
                .addScript("classpath:db/postgresql/schema.sql")
                .addScript("classpath:db/h2/fixtures.sql")
                .build();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(database);
        TransactionTemplate transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(database));
        repository = new JdbcWalletRepository(jdbcTemplate, transactionTemplate);
        commandService = new InMemoryWalletCommandService(
                Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
                repository
        );
        ledgerQueryService = new InMemoryWalletLedgerQueryService(repository, repository);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void readsSeededWalletState() {
        assertThat(repository.findMember("member-001")).isPresent();
        assertThat(repository.findWalletAccount("wallet-001")).isPresent();
        assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("125000"));
        assertThat(repository.findTransactions("wallet-001")).hasSize(2);
    }

    @Test
    void chargePersistsBalanceTransactionLedgerAuditAndOperation() {
        WalletCommandResult result = commandService.charge(
                "wallet-001",
                new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전")
        );

        assertThat(result.created()).isTrue();
        assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("130000"));
        assertThat(repository.findTransactions("wallet-001"))
                .anySatisfy(transaction -> assertThat(transaction.transactionId()).isEqualTo("txn-003"));
        assertThat(ledgerQueryService.getLedgerEntries("wallet-001"))
                .singleElement()
                .satisfies(ledgerEntry -> {
                    assertThat(ledgerEntry.operationId()).isEqualTo("op-001");
                    assertThat(ledgerEntry.balanceAfter()).isEqualTo(money("130000"));
                    assertThat(ledgerEntry.type()).isEqualTo(TransactionType.CHARGE);
                });
        assertThat(ledgerQueryService.getAuditEvents()).singleElement()
                .satisfies(auditEvent -> assertThat(auditEvent.operationId()).isEqualTo("op-001"));
        assertThat(repository.findOperationStepLogs("op-001"))
                .extracting(stepLog -> stepLog.step())
                .containsExactly(
                        OperationStep.BALANCE_LOCKED,
                        OperationStep.BALANCE_UPDATED,
                        OperationStep.TRANSACTION_RECORDED,
                        OperationStep.LEDGER_RECORDED,
                        OperationStep.AUDIT_RECORDED,
                        OperationStep.IDEMPOTENCY_RECORDED
                );
        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.eventType()).isEqualTo("CHARGE_COMPLETED");
                    assertThat(outboxEvent.aggregateType()).isEqualTo("WALLET_OPERATION");
                    assertThat(outboxEvent.aggregateId()).isEqualTo("op-001");
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PENDING);
                    assertThat(outboxEvent.attemptCount()).isZero();
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.claimedAt()).isNull();
                    assertThat(outboxEvent.leaseExpiresAt()).isNull();
                    assertThat(outboxEvent.publishedAt()).isNull();
                    assertThat(outboxEvent.lastError()).isNull();
                    assertThat(outboxEvent.payload()).contains("\"operationId\":\"op-001\"");
                });
        assertThat(repository.findOperation("charge-db-001")).isPresent();
    }

    @Test
    void repeatedChargeWithSameIdempotencyKeyDoesNotDuplicateLedgerAuditOrBalance() {
        WalletChargeCommand command = new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전");

        WalletCommandResult first = commandService.charge("wallet-001", command);
        WalletCommandResult second = commandService.charge("wallet-001", command);

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.operation().operationId()).isEqualTo(first.operation().operationId());
        assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("130000"));
        assertThat(ledgerQueryService.getLedgerEntries("wallet-001")).hasSize(1);
        assertThat(ledgerQueryService.getAuditEvents()).hasSize(1);
        assertThat(repository.findOperationStepLogs("op-001")).hasSize(6);
        assertThat(repository.findOperationOutboxEvents("op-001")).hasSize(1);
    }

    @Test
    void outboxRelayStateTransitions() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));

        assertThat(repository.findPendingOutboxEvents(10))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.outboxEventId()).isEqualTo("outbox-001"));

        repository.markOutboxEventPublished("outbox-001", Instant.parse("2026-05-01T00:01:00Z"));

        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PUBLISHED);
                    assertThat(outboxEvent.publishedAt()).isEqualTo(Instant.parse("2026-05-01T00:01:00Z"));
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.claimedAt()).isNull();
                    assertThat(outboxEvent.leaseExpiresAt()).isNull();
                    assertThat(outboxEvent.lastError()).isNull();
                });
        assertThat(repository.findPendingOutboxEvents(10)).isEmpty();
    }

    @Test
    void outboxRelayFailureIncrementsAttemptCount() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));

        repository.markOutboxEventFailed(
                "outbox-001",
                "broker unavailable",
                Instant.parse("2026-05-01T00:01:30Z"),
                3
        );

        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.FAILED);
                    assertThat(outboxEvent.attemptCount()).isEqualTo(1);
                    assertThat(outboxEvent.nextRetryAt()).isEqualTo(Instant.parse("2026-05-01T00:01:30Z"));
                    assertThat(outboxEvent.claimedAt()).isNull();
                    assertThat(outboxEvent.leaseExpiresAt()).isNull();
                    assertThat(outboxEvent.lastError()).isEqualTo("broker unavailable");
                    assertThat(outboxEvent.publishedAt()).isNull();
                });
        assertThat(repository.findPendingOutboxEvents(10)).isEmpty();
    }

    @Test
    void outboxClaimMovesReadyEventsToProcessing() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));
        commandService.transfer("wallet-001", new WalletTransferCommand("wallet-002", money("1000"), "transfer-db-001", "DB 송금"));

        assertThat(repository.claimReadyOutboxEvents(
                1,
                Instant.parse("2026-05-01T00:01:00Z"),
                Instant.parse("2026-05-01T00:02:00Z")
        ))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.outboxEventId()).isEqualTo("outbox-001");
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING);
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.claimedAt()).isEqualTo(Instant.parse("2026-05-01T00:01:00Z"));
                    assertThat(outboxEvent.leaseExpiresAt()).isEqualTo(Instant.parse("2026-05-01T00:02:00Z"));
                    assertThat(outboxEvent.lastError()).isNull();
                });
        assertThat(repository.findPendingOutboxEvents(10))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.outboxEventId()).isEqualTo("outbox-002"));
    }

    @Test
    void outboxClaimWaitsUntilFailedEventRetryTime() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));
        repository.markOutboxEventFailed(
                "outbox-001",
                "broker unavailable",
                Instant.parse("2026-05-01T00:01:30Z"),
                3
        );

        assertThat(repository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:01:00Z"),
                Instant.parse("2026-05-01T00:02:00Z")
        )).isEmpty();
        assertThat(repository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:01:30Z"),
                Instant.parse("2026-05-01T00:02:30Z")
        ))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING);
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.claimedAt()).isEqualTo(Instant.parse("2026-05-01T00:01:30Z"));
                    assertThat(outboxEvent.leaseExpiresAt()).isEqualTo(Instant.parse("2026-05-01T00:02:30Z"));
                    assertThat(outboxEvent.lastError()).isNull();
                });
    }

    @Test
    void outboxClaimRecoversExpiredProcessingEvent() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));
        repository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:01:00Z"),
                Instant.parse("2026-05-01T00:02:00Z")
        );

        assertThat(repository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:01:59Z"),
                Instant.parse("2026-05-01T00:02:59Z")
        )).isEmpty();
        assertThat(repository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:02:00Z"),
                Instant.parse("2026-05-01T00:03:00Z")
        ))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING);
                    assertThat(outboxEvent.claimedAt()).isEqualTo(Instant.parse("2026-05-01T00:02:00Z"));
                    assertThat(outboxEvent.leaseExpiresAt()).isEqualTo(Instant.parse("2026-05-01T00:03:00Z"));
                });
    }

    @Test
    void stalePublishedWriterCannotOverwriteReclaimedOutboxEvent() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));
        var firstClaim = repository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:01:00Z"),
                Instant.parse("2026-05-01T00:02:00Z")
        ).getFirst();
        var secondClaim = repository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:02:00Z"),
                Instant.parse("2026-05-01T00:03:00Z")
        ).getFirst();

        assertThatThrownBy(() -> repository.markClaimedOutboxEventPublished(
                firstClaim.outboxEventId(),
                firstClaim.claimedAt(),
                firstClaim.leaseExpiresAt(),
                Instant.parse("2026-05-01T00:02:01Z")
        ))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("outbox event claim is no longer active: outbox-001");

        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING);
                    assertThat(outboxEvent.claimedAt()).isEqualTo(secondClaim.claimedAt());
                    assertThat(outboxEvent.leaseExpiresAt()).isEqualTo(secondClaim.leaseExpiresAt());
                    assertThat(outboxEvent.publishedAt()).isNull();
                });
    }

    @Test
    void staleFailedWriterCannotOverwriteReclaimedOutboxEvent() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));
        var firstClaim = repository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:01:00Z"),
                Instant.parse("2026-05-01T00:02:00Z")
        ).getFirst();
        var secondClaim = repository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:02:00Z"),
                Instant.parse("2026-05-01T00:03:00Z")
        ).getFirst();

        assertThatThrownBy(() -> repository.markClaimedOutboxEventFailed(
                firstClaim.outboxEventId(),
                firstClaim.claimedAt(),
                firstClaim.leaseExpiresAt(),
                "stale broker failure",
                Instant.parse("2026-05-01T00:02:31Z"),
                3
        ))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("outbox event claim is no longer active: outbox-001");

        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING);
                    assertThat(outboxEvent.attemptCount()).isZero();
                    assertThat(outboxEvent.claimedAt()).isEqualTo(secondClaim.claimedAt());
                    assertThat(outboxEvent.leaseExpiresAt()).isEqualTo(secondClaim.leaseExpiresAt());
                    assertThat(outboxEvent.lastError()).isNull();
                });
    }

    @Test
    void outboxFailureMovesToManualReviewAtMaxAttempts() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));

        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:01:30Z"), 3);
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:02:30Z"), 3);
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:03:30Z"), 3);

        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.MANUAL_REVIEW);
                    assertThat(outboxEvent.attemptCount()).isEqualTo(3);
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.claimedAt()).isNull();
                    assertThat(outboxEvent.leaseExpiresAt()).isNull();
                    assertThat(outboxEvent.lastError()).isEqualTo("broker unavailable");
                });
        assertThat(repository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:10:00Z"),
                Instant.parse("2026-05-01T00:11:00Z")
        )).isEmpty();
    }

    @Test
    void outboxManualReviewCanBeRequeued() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:01:30Z"), 3);
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:02:30Z"), 3);
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:03:30Z"), 3);

        assertThat(repository.findManualReviewOutboxEvents(10))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.MANUAL_REVIEW));

        repository.requeueManualReviewOutboxEvent(
                "outbox-001",
                Instant.parse("2026-05-01T00:10:00Z"),
                "ops-user",
                "broker recovered"
        );

        assertThat(repository.findManualReviewOutboxEvents(10)).isEmpty();
        assertThat(repository.findOutboxRequeueAudits("outbox-001"))
                .singleElement()
                .satisfies(audit -> {
                    assertThat(audit.auditId()).isEqualTo("outbox-requeue-audit-001");
                    assertThat(audit.outboxEventId()).isEqualTo("outbox-001");
                    assertThat(audit.operationId()).isEqualTo("op-001");
                    assertThat(audit.requeuedAt()).isEqualTo(Instant.parse("2026-05-01T00:10:00Z"));
                    assertThat(audit.operator()).isEqualTo("ops-user");
                    assertThat(audit.reason()).isEqualTo("broker recovered");
                });
        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PENDING);
                    assertThat(outboxEvent.attemptCount()).isZero();
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.lastError()).isNull();
                });
        assertThat(repository.claimReadyOutboxEvents(
                10,
                Instant.parse("2026-05-01T00:10:00Z"),
                Instant.parse("2026-05-01T00:11:00Z")
        ))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING));
    }

    @Test
    void outboxManualReviewRequeueCanBeRequestedApprovedAndExecuted() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:01:30Z"), 3);
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:02:30Z"), 3);
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:03:30Z"), 3);

        var requested = repository.requestManualReviewRequeue(
                "outbox-001",
                Instant.parse("2026-05-01T00:10:00Z"),
                "ops-requester",
                "broker recovered"
        );

        assertThat(requested.requestId()).isEqualTo("outbox-requeue-request-001");
        assertThat(requested.status()).isEqualTo(OperationOutboxRequeueRequestStatus.REQUESTED);

        var approved = repository.approveManualReviewRequeueRequest(
                requested.requestId(),
                Instant.parse("2026-05-01T00:11:00Z"),
                "ops-approver",
                "원인 조치 확인"
        );

        assertThat(approved.status()).isEqualTo(OperationOutboxRequeueRequestStatus.APPROVED);
        assertThat(approved.approvedBy()).isEqualTo("ops-approver");

        var executed = repository.executeManualReviewRequeueRequest(
                requested.requestId(),
                Instant.parse("2026-05-01T00:12:00Z"),
                "ops-executor"
        );

        assertThat(executed.status()).isEqualTo(OperationOutboxRequeueRequestStatus.EXECUTED);
        assertThat(executed.executedBy()).isEqualTo("ops-executor");
        assertThat(repository.findOutboxRequeueRequests("outbox-001"))
                .singleElement()
                .satisfies(request -> {
                    assertThat(request.status()).isEqualTo(OperationOutboxRequeueRequestStatus.EXECUTED);
                    assertThat(request.requestedBy()).isEqualTo("ops-requester");
                    assertThat(request.approvedBy()).isEqualTo("ops-approver");
                });
        assertThat(repository.findOutboxRequeueAudits("outbox-001"))
                .singleElement()
                .satisfies(audit -> {
                    assertThat(audit.requeuedAt()).isEqualTo(Instant.parse("2026-05-01T00:12:00Z"));
                    assertThat(audit.operator()).isEqualTo("ops-executor");
                    assertThat(audit.reason()).isEqualTo("broker recovered");
                });
        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PENDING));
    }

    @Test
    void outboxManualReviewRequeueRequestCanBeRejected() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:01:30Z"), 3);
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:02:30Z"), 3);
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:03:30Z"), 3);
        var requested = repository.requestManualReviewRequeue(
                "outbox-001",
                Instant.parse("2026-05-01T00:10:00Z"),
                "ops-requester",
                "broker recovered"
        );

        var rejected = repository.rejectManualReviewRequeueRequest(
                requested.requestId(),
                Instant.parse("2026-05-01T00:11:00Z"),
                "ops-rejector",
                "원인 조치 미확인"
        );

        assertThat(rejected.status()).isEqualTo(OperationOutboxRequeueRequestStatus.REJECTED);
        assertThat(rejected.rejectedBy()).isEqualTo("ops-rejector");
        assertThat(rejected.rejectedAt()).isEqualTo(Instant.parse("2026-05-01T00:11:00Z"));
        assertThat(rejected.rejectionReason()).isEqualTo("원인 조치 미확인");
        assertThat(repository.findOutboxRequeueAudits("outbox-001")).isEmpty();
        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.MANUAL_REVIEW));
    }

    @Test
    void recordsAndReturnsRecentOutboxRelayRuns() {
        repository.saveOutboxRelayRun(new OperationOutboxRelayRun(
                repository.nextRelayRunId(),
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:01Z"),
                OperationOutboxRelayRunStatus.SUCCESS,
                10,
                3,
                2,
                1,
                null
        ));
        repository.saveOutboxRelayRun(new OperationOutboxRelayRun(
                repository.nextRelayRunId(),
                Instant.parse("2026-05-01T00:00:02Z"),
                Instant.parse("2026-05-01T00:00:03Z"),
                OperationOutboxRelayRunStatus.FAILED,
                10,
                0,
                0,
                0,
                "publisher down"
        ));

        assertThat(repository.findRecentOutboxRelayRuns(1))
                .singleElement()
                .satisfies(relayRun -> {
                    assertThat(relayRun.relayRunId()).isEqualTo("outbox-relay-run-002");
                    assertThat(relayRun.status()).isEqualTo(OperationOutboxRelayRunStatus.FAILED);
                    assertThat(relayRun.errorMessage()).isEqualTo("publisher down");
                });
    }

    @Test
    void deletesOutboxRelayRunsCompletedBeforeCutoff() {
        repository.saveOutboxRelayRun(new OperationOutboxRelayRun(
                repository.nextRelayRunId(),
                Instant.parse("2026-04-30T23:59:58Z"),
                Instant.parse("2026-04-30T23:59:59Z"),
                OperationOutboxRelayRunStatus.SUCCESS,
                10,
                0,
                0,
                0,
                null
        ));
        repository.saveOutboxRelayRun(new OperationOutboxRelayRun(
                repository.nextRelayRunId(),
                Instant.parse("2026-04-30T23:59:59Z"),
                Instant.parse("2026-05-01T00:00:00Z"),
                OperationOutboxRelayRunStatus.SUCCESS,
                10,
                0,
                0,
                0,
                null
        ));

        int deletedCount = repository.deleteOutboxRelayRunsCompletedBefore(Instant.parse("2026-05-01T00:00:00Z"));

        assertThat(deletedCount).isEqualTo(1);
        assertThat(repository.findRecentOutboxRelayRuns(10))
                .singleElement()
                .satisfies(relayRun -> assertThat(relayRun.relayRunId()).isEqualTo("outbox-relay-run-002"));
    }

    @Test
    void recordsAndReturnsRecentAdminApiAccessAudits() {
        repository.saveAdminApiAccessAudit(new AdminApiAccessAudit(
                repository.nextAdminApiAccessAuditId(),
                Instant.parse("2026-05-01T00:00:00Z"),
                "GET",
                "/api/v1/outbox-relay-runs",
                "ops-user",
                200,
                AdminApiAccessOutcome.SUCCESS
        ));
        repository.saveAdminApiAccessAudit(new AdminApiAccessAudit(
                repository.nextAdminApiAccessAuditId(),
                Instant.parse("2026-05-01T00:00:01Z"),
                "GET",
                "/api/v1/outbox-relay-runs",
                null,
                401,
                AdminApiAccessOutcome.FAILURE
        ));

        assertThat(repository.findRecentAdminApiAccessAudits(1))
                .singleElement()
                .satisfies(accessAudit -> {
                    assertThat(accessAudit.auditId()).isEqualTo("admin-api-access-audit-002");
                    assertThat(accessAudit.operatorId()).isNull();
                    assertThat(accessAudit.statusCode()).isEqualTo(401);
                    assertThat(accessAudit.outcome()).isEqualTo(AdminApiAccessOutcome.FAILURE);
                });
    }

    @Test
    void deletesAdminApiAccessAuditsOccurredBeforeCutoff() {
        repository.saveAdminApiAccessAudit(new AdminApiAccessAudit(
                repository.nextAdminApiAccessAuditId(),
                Instant.parse("2026-04-30T23:59:59Z"),
                "GET",
                "/api/v1/outbox-relay-runs",
                "ops-user",
                200,
                AdminApiAccessOutcome.SUCCESS
        ));
        repository.saveAdminApiAccessAudit(new AdminApiAccessAudit(
                repository.nextAdminApiAccessAuditId(),
                Instant.parse("2026-05-01T00:00:00Z"),
                "GET",
                "/api/v1/outbox-relay-runs",
                "ops-user",
                200,
                AdminApiAccessOutcome.SUCCESS
        ));

        int deletedCount = repository.deleteAdminApiAccessAuditsOccurredBefore(
                Instant.parse("2026-05-01T00:00:00Z")
        );

        assertThat(deletedCount).isEqualTo(1);
        assertThat(repository.findRecentAdminApiAccessAudits(10))
                .singleElement()
                .satisfies(accessAudit -> assertThat(accessAudit.auditId()).isEqualTo("admin-api-access-audit-002"));
    }

    @Test
    void consumerProcessedEventDedupeRecordsOnlyFirstEvent() {
        boolean firstRecorded = repository.recordProcessedEvent(
                "outbox-001",
                "outbox-001",
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:05:00Z")
        );
        boolean duplicateRecorded = repository.recordProcessedEvent(
                "outbox-001",
                "outbox-001",
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:06:00Z")
        );

        assertThat(firstRecorded).isTrue();
        assertThat(duplicateRecorded).isFalse();
        assertThat(repository.findProcessedEvent("outbox-001"))
                .hasValueSatisfying(processedEvent -> {
                    assertThat(processedEvent.outboxEventId()).isEqualTo("outbox-001");
                    assertThat(processedEvent.eventType()).isEqualTo("CHARGE_COMPLETED");
                    assertThat(processedEvent.processedAt()).isEqualTo(Instant.parse("2026-05-01T00:05:00Z"));
                    assertThat(processedEvent.duplicateCount()).isEqualTo(1);
                });
    }

    @Test
    void consumerMonitoringMetricsCountsProcessedDuplicatesAndReceipts() {
        repository.recordProcessedEvent(
                "outbox-001",
                "outbox-001",
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:05:00Z")
        );
        repository.recordProcessedEvent(
                "outbox-001",
                "outbox-001",
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:06:00Z")
        );
        repository.saveConsumerReceipt(new com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt(
                "outbox-001",
                "outbox-001",
                "op-001",
                "CHARGE_COMPLETED",
                "WALLET_OPERATION",
                "op-001",
                Instant.parse("2026-05-01T00:05:30Z")
        ));

        assertThat(repository.getConsumerMetrics())
                .satisfies(metrics -> {
                    assertThat(metrics.processedEventCount()).isEqualTo(1);
                    assertThat(metrics.duplicateEventCount()).isEqualTo(1);
                    assertThat(metrics.receiptCount()).isEqualTo(1);
                    assertThat(metrics.lastProcessedAt()).isEqualTo(Instant.parse("2026-05-01T00:05:00Z"));
                    assertThat(metrics.lastReceivedAt()).isEqualTo(Instant.parse("2026-05-01T00:05:30Z"));
                });
        assertThat(repository.findRecentConsumerReceipts(10))
                .singleElement()
                .satisfies(receipt -> assertThat(receipt.idempotencyKey()).isEqualTo("outbox-001"));
    }

    @Test
    void sameIdempotencyKeyWithDifferentRequestFails() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));

        assertThatThrownBy(() -> commandService.charge(
                "wallet-001",
                new WalletChargeCommand(money("6000"), "charge-db-001", "DB 충전")
        ))
                .isInstanceOf(IdempotencyKeyConflictException.class)
                .hasMessage("Idempotency key already used for different request: charge-db-001");
    }

    @Test
    void transferPersistsBothWalletBalancesAndLedgerEntries() {
        WalletCommandResult result = commandService.transfer(
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("25000"), "transfer-db-001", "DB 송금")
        );

        assertThat(result.created()).isTrue();
        assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("100000"));
        assertThat(repository.findBalance("wallet-002").orElseThrow().money()).isEqualTo(money("55000"));
        assertThat(ledgerQueryService.getLedgerEntries("wallet-001"))
                .singleElement()
                .satisfies(ledgerEntry -> {
                    assertThat(ledgerEntry.direction()).isEqualTo(TransactionDirection.DEBIT);
                    assertThat(ledgerEntry.balanceAfter()).isEqualTo(money("100000"));
                });
        assertThat(ledgerQueryService.getLedgerEntries("wallet-002"))
                .singleElement()
                .satisfies(ledgerEntry -> {
                    assertThat(ledgerEntry.direction()).isEqualTo(TransactionDirection.CREDIT);
                    assertThat(ledgerEntry.balanceAfter()).isEqualTo(money("55000"));
                });
        assertThat(repository.findOperationStepLogs(result.operation().operationId()))
                .extracting(stepLog -> stepLog.step())
                .containsExactly(
                        OperationStep.BALANCE_LOCKED,
                        OperationStep.BALANCE_UPDATED,
                        OperationStep.TRANSACTION_RECORDED,
                        OperationStep.LEDGER_RECORDED,
                        OperationStep.AUDIT_RECORDED,
                        OperationStep.IDEMPOTENCY_RECORDED
                );
        assertThat(repository.findOperationOutboxEvents(result.operation().operationId()))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.eventType()).isEqualTo("TRANSFER_COMPLETED");
                    assertThat(outboxEvent.payload()).contains("\"counterpartyWalletId\":\"wallet-002\"");
                });
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), "KRW");
    }
}
