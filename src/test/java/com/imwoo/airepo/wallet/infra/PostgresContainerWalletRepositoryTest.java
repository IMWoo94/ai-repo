package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.application.InsufficientBalanceException;
import com.imwoo.airepo.wallet.application.InMemoryWalletCommandService;
import com.imwoo.airepo.wallet.application.InvalidWalletOperationException;
import com.imwoo.airepo.wallet.application.WalletConcurrencyException;
import com.imwoo.airepo.wallet.application.InMemoryWalletLedgerQueryService;
import com.imwoo.airepo.wallet.application.WalletChargeCommand;
import com.imwoo.airepo.wallet.application.WalletCommandResult;
import com.imwoo.airepo.wallet.application.WalletOperationOutcome;
import com.imwoo.airepo.wallet.application.WalletTransferCommand;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueRequestStatus;
import com.imwoo.airepo.wallet.domain.OperationOutboxStatus;
import com.imwoo.airepo.wallet.domain.TransactionDirection;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@Tag("postgres-scenario")
class PostgresContainerWalletRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine")
    )
            .withDatabaseName("ai_repo")
            .withUsername("ai_repo")
            .withPassword("ai_repo");

    private JdbcWalletRepository repository;
    private InMemoryWalletCommandService commandService;
    private InMemoryWalletLedgerQueryService ledgerQueryService;
    private DriverManagerDataSource dataSource;

    @BeforeEach
    void setUp() {
        dataSource = new DriverManagerDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());

        resetDatabase(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        repository = new JdbcWalletRepository(
                jdbcTemplate,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        );
        commandService = new InMemoryWalletCommandService(
                Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
                repository
        );
        ledgerQueryService = new InMemoryWalletLedgerQueryService(repository, repository);
    }

    @Test
    void chargePersistsThroughRealPostgres() {
        WalletCommandResult result = commandService.charge(
                "wallet-001",
                new WalletChargeCommand(money("5000"), "postgres-charge-001", "PostgreSQL 충전")
        );

        assertThat(result.created()).isTrue();
        assertThat(result.operation().operationId()).isEqualTo("op-001");
        assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("130000"));
        assertThat(ledgerQueryService.getLedgerEntries("wallet-001"))
                .singleElement()
                .satisfies(ledgerEntry -> assertThat(ledgerEntry.balanceAfter()).isEqualTo(money("130000")));
        assertThat(ledgerQueryService.getAuditEvents()).singleElement()
                .satisfies(auditEvent -> assertThat(auditEvent.operationId()).isEqualTo("op-001"));
        assertThat(repository.findOperationStepLogs("op-001")).hasSize(6);
        assertThat(repository.findOperationOutboxEvents("op-001")).hasSize(1);
    }

    @Test
    void transferPersistsThroughRealPostgres() {
        WalletCommandResult result = commandService.transfer(
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("25000"), "postgres-transfer-001", "PostgreSQL 송금")
        );

        assertThat(result.created()).isTrue();
        assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("100000"));
        assertThat(repository.findBalance("wallet-002").orElseThrow().money()).isEqualTo(money("55000"));
        assertThat(ledgerQueryService.getLedgerEntries("wallet-001"))
                .singleElement()
                .satisfies(ledgerEntry -> assertThat(ledgerEntry.direction()).isEqualTo(TransactionDirection.DEBIT));
        assertThat(ledgerQueryService.getLedgerEntries("wallet-002"))
                .singleElement()
                .satisfies(ledgerEntry -> assertThat(ledgerEntry.direction()).isEqualTo(TransactionDirection.CREDIT));
        assertThat(repository.findOperationStepLogs(result.operation().operationId())).hasSize(6);
        assertThat(repository.findOperationOutboxEvents(result.operation().operationId())).hasSize(1);
    }

    @Test
    void idempotentRetryDoesNotDuplicateLedgerOrAuditInRealPostgres() {
        WalletChargeCommand command = new WalletChargeCommand(
                money("5000"),
                "postgres-charge-001",
                "PostgreSQL 충전"
        );

        WalletCommandResult first = commandService.charge("wallet-001", command);
        WalletCommandResult second = commandService.charge("wallet-001", command);

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.operation().operationId()).isEqualTo(first.operation().operationId());
        assertThat(ledgerQueryService.getLedgerEntries("wallet-001")).hasSize(1);
        assertThat(ledgerQueryService.getAuditEvents()).hasSize(1);
        assertThat(repository.findOperationStepLogs(first.operation().operationId())).hasSize(6);
        assertThat(repository.findOperationOutboxEvents(first.operation().operationId())).hasSize(1);
    }

    @Test
    void concurrentTransfersLockBalanceRowsAndPreventOverdraft() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Future<TransferAttempt> firstAttempt = executorService.submit(() -> applyConcurrentTransfer(
                    startLatch,
                    "postgres-concurrent-transfer-001",
                    "postgres-concurrent-transfer-fingerprint-001"
            ));
            Future<TransferAttempt> secondAttempt = executorService.submit(() -> applyConcurrentTransfer(
                    startLatch,
                    "postgres-concurrent-transfer-002",
                    "postgres-concurrent-transfer-fingerprint-002"
            ));

            startLatch.countDown();

            List<TransferAttempt> attempts = List.of(
                    firstAttempt.get(10, TimeUnit.SECONDS),
                    secondAttempt.get(10, TimeUnit.SECONDS)
            );

            assertThat(attempts).filteredOn(TransferAttempt::successful).hasSize(1);
            assertThat(attempts)
                    .filteredOn(attempt -> !attempt.successful())
                    .singleElement()
                    .satisfies(attempt -> assertThat(attempt.exception())
                            .isInstanceOf(InsufficientBalanceException.class));
            assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("45000"));
            assertThat(repository.findBalance("wallet-002").orElseThrow().money()).isEqualTo(money("110000"));
            assertThat(ledgerQueryService.getLedgerEntries("wallet-001")).hasSize(1);
            assertThat(ledgerQueryService.getLedgerEntries("wallet-002")).hasSize(1);
            assertThat(ledgerQueryService.getAuditEvents()).hasSize(1);
            assertThat(
                    repository.findOperation("postgres-concurrent-transfer-001").isPresent()
                            ^ repository.findOperation("postgres-concurrent-transfer-002").isPresent()
            ).isTrue();
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void concurrentChargesWithSameIdempotencyKeyRecoverIdempotentResultInsteadOfFailing() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Future<ChargeAttempt> firstAttempt = executorService.submit(() -> applyConcurrentCharge(startLatch));
            Future<ChargeAttempt> secondAttempt = executorService.submit(() -> applyConcurrentCharge(startLatch));

            startLatch.countDown();

            List<ChargeAttempt> attempts = List.of(
                    firstAttempt.get(10, TimeUnit.SECONDS),
                    secondAttempt.get(10, TimeUnit.SECONDS)
            );

            assertThat(attempts).filteredOn(ChargeAttempt::successful).hasSize(2);
            assertThat(attempts).extracting(ChargeAttempt::operationId).containsOnly("op-001");
            assertThat(attempts).filteredOn(ChargeAttempt::created).hasSize(1);
            assertThat(attempts).filteredOn(attempt -> !attempt.created()).hasSize(1);
            assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("130000"));
            assertThat(ledgerQueryService.getLedgerEntries("wallet-001")).hasSize(1);
            assertThat(ledgerQueryService.getAuditEvents()).hasSize(1);
            assertThat(repository.findOperationStepLogs("op-001")).hasSize(6);
            assertThat(repository.findOperationOutboxEvents("op-001")).hasSize(1);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void lockTimeoutReturnsWalletConcurrencyExceptionWithoutWritingRecords() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        CountDownLatch lockedLatch = new CountDownLatch(1);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        Future<?> lockHolder = executorService.submit(() -> holdWalletBalanceLock(lockedLatch, releaseLatch));

        try {
            assertThat(lockedLatch.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> repository.applyCharge(
                    "postgres-lock-timeout-001",
                    "postgres-lock-timeout-fingerprint-001",
                    "wallet-001",
                    money("5000"),
                    "PostgreSQL lock timeout 충전",
                    Instant.parse("2026-05-01T00:00:00Z")
            ))
                    .isInstanceOf(WalletConcurrencyException.class)
                    .hasMessage("Wallet balance is busy. Please retry.");

            assertThat(repository.findBalance("wallet-001").orElseThrow().money()).isEqualTo(money("125000"));
            assertThat(ledgerQueryService.getLedgerEntries("wallet-001")).isEmpty();
            assertThat(ledgerQueryService.getAuditEvents()).isEmpty();
            assertThat(repository.findOperationStepLogs("op-001")).isEmpty();
            assertThat(repository.findOperationOutboxEvents("op-001")).isEmpty();
            assertThat(repository.findOperation("postgres-lock-timeout-001")).isEmpty();
        } finally {
            releaseLatch.countDown();
            lockHolder.get(5, TimeUnit.SECONDS);
            executorService.shutdownNow();
        }
    }

    @Test
    void concurrentRequeueExecutionsOnlyExecuteOnceInRealPostgres() throws Exception {
        makeManualReviewOutboxEvent();
        var requested = repository.requestManualReviewRequeue(
                "outbox-001",
                Instant.parse("2026-05-01T00:10:00Z"),
                "ops-requester",
                "broker recovered"
        );
        repository.approveManualReviewRequeueRequest(
                requested.requestId(),
                Instant.parse("2026-05-01T00:11:00Z"),
                "ops-approver",
                "원인 조치 확인"
        );
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Future<RequeueAttempt> firstAttempt = executorService.submit(() -> executeRequeueConcurrently(
                    startLatch,
                    requested.requestId(),
                    "ops-executor-1"
            ));
            Future<RequeueAttempt> secondAttempt = executorService.submit(() -> executeRequeueConcurrently(
                    startLatch,
                    requested.requestId(),
                    "ops-executor-2"
            ));

            startLatch.countDown();

            List<RequeueAttempt> attempts = List.of(
                    firstAttempt.get(10, TimeUnit.SECONDS),
                    secondAttempt.get(10, TimeUnit.SECONDS)
            );

            assertThat(attempts).filteredOn(RequeueAttempt::successful).hasSize(1);
            assertThat(attempts)
                    .filteredOn(attempt -> !attempt.successful())
                    .singleElement()
                    .satisfies(attempt -> assertThat(attempt.exception())
                            .isInstanceOf(InvalidWalletOperationException.class)
                            .hasMessage("requeue request must be APPROVED: " + requested.requestId()));
            assertThat(repository.findOutboxRequeueRequests("outbox-001"))
                    .singleElement()
                    .satisfies(request -> assertThat(request.status()).isEqualTo(OperationOutboxRequeueRequestStatus.EXECUTED));
            assertThat(repository.findOutboxRequeueAudits("outbox-001")).hasSize(1);
            assertThat(repository.findOperationOutboxEvents("op-001"))
                    .singleElement()
                    .satisfies(outboxEvent -> assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PENDING));
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void staleOutboxWriterCannotOverwriteReclaimedEventInRealPostgres() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "postgres-stale-outbox-001", "PostgreSQL stale outbox 충전"));
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
    void concurrentRequeueApproveAndRejectOnlyOneTransitionWinsInRealPostgres() throws Exception {
        makeManualReviewOutboxEvent();
        var requested = repository.requestManualReviewRequeue(
                "outbox-001",
                Instant.parse("2026-05-01T00:10:00Z"),
                "ops-requester",
                "broker recovered"
        );
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Future<RequeueAttempt> approveAttempt = executorService.submit(() -> approveRequeueConcurrently(
                    startLatch,
                    requested.requestId()
            ));
            Future<RequeueAttempt> rejectAttempt = executorService.submit(() -> rejectRequeueConcurrently(
                    startLatch,
                    requested.requestId()
            ));

            startLatch.countDown();

            List<RequeueAttempt> attempts = List.of(
                    approveAttempt.get(10, TimeUnit.SECONDS),
                    rejectAttempt.get(10, TimeUnit.SECONDS)
            );

            assertThat(attempts).filteredOn(RequeueAttempt::successful).hasSize(1);
            assertThat(attempts)
                    .filteredOn(attempt -> !attempt.successful())
                    .singleElement()
                    .satisfies(attempt -> assertThat(attempt.exception())
                            .isInstanceOf(InvalidWalletOperationException.class));
            assertThat(repository.findOutboxRequeueRequests("outbox-001"))
                    .singleElement()
                    .satisfies(request -> assertThat(request.status())
                            .isIn(OperationOutboxRequeueRequestStatus.APPROVED, OperationOutboxRequeueRequestStatus.REJECTED));
            assertThat(repository.findOutboxRequeueAudits("outbox-001")).isEmpty();
            assertThat(repository.findOperationOutboxEvents("op-001"))
                    .singleElement()
                    .satisfies(outboxEvent -> assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.MANUAL_REVIEW));
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void concurrentConsumerProcessedEventDedupeOnlyRecordsOnceInRealPostgres() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        try {
            Future<ConsumerProcessedAttempt> firstAttempt = executorService.submit(() -> recordConsumerProcessedEventConcurrently(
                    startLatch,
                    Instant.parse("2026-05-01T00:05:00Z")
            ));
            Future<ConsumerProcessedAttempt> secondAttempt = executorService.submit(() -> recordConsumerProcessedEventConcurrently(
                    startLatch,
                    Instant.parse("2026-05-01T00:06:00Z")
            ));

            startLatch.countDown();

            List<ConsumerProcessedAttempt> attempts = List.of(
                    firstAttempt.get(10, TimeUnit.SECONDS),
                    secondAttempt.get(10, TimeUnit.SECONDS)
            );

            assertThat(attempts).filteredOn(ConsumerProcessedAttempt::recorded).hasSize(1);
            assertThat(attempts).filteredOn(attempt -> !attempt.recorded()).hasSize(1);
            assertThat(repository.findProcessedEvent("outbox-001"))
                    .hasValueSatisfying(processedEvent -> {
                    assertThat(processedEvent.outboxEventId()).isEqualTo("outbox-001");
                    assertThat(processedEvent.eventType()).isEqualTo("CHARGE_COMPLETED");
                    assertThat(processedEvent.duplicateCount()).isEqualTo(1);
                });
        } finally {
            executorService.shutdownNow();
        }
    }

    private Void holdWalletBalanceLock(CountDownLatch lockedLatch, CountDownLatch releaseLatch) {
        TransactionTemplate lockTransaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        lockTransaction.execute(status -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.queryForObject(
                    "select wallet_id from wallet_balances where wallet_id = ? for update",
                    String.class,
                    "wallet-001"
            );
            lockedLatch.countDown();
            awaitRelease(releaseLatch);
            return null;
        });
        return null;
    }

    private void awaitRelease(CountDownLatch releaseLatch) {
        try {
            if (!releaseLatch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release wallet balance lock");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while holding wallet balance lock", exception);
        }
    }

    private void makeManualReviewOutboxEvent() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "postgres-requeue-charge-001", "PostgreSQL requeue 충전"));
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:01:30Z"), 3);
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:02:30Z"), 3);
        repository.markOutboxEventFailed("outbox-001", "broker unavailable", Instant.parse("2026-05-01T00:03:30Z"), 3);
    }

    private RequeueAttempt executeRequeueConcurrently(
            CountDownLatch startLatch,
            String requestId,
            String executor
    ) throws InterruptedException {
        awaitConcurrentStart(startLatch);

        try {
            repository.executeManualReviewRequeueRequest(
                    requestId,
                    Instant.parse("2026-05-01T00:12:00Z"),
                    executor
            );
            return RequeueAttempt.success();
        } catch (RuntimeException exception) {
            return RequeueAttempt.failure(exception);
        }
    }

    private RequeueAttempt approveRequeueConcurrently(
            CountDownLatch startLatch,
            String requestId
    ) throws InterruptedException {
        awaitConcurrentStart(startLatch);

        try {
            repository.approveManualReviewRequeueRequest(
                    requestId,
                    Instant.parse("2026-05-01T00:11:00Z"),
                    "ops-approver",
                    "원인 조치 확인"
            );
            return RequeueAttempt.success();
        } catch (RuntimeException exception) {
            return RequeueAttempt.failure(exception);
        }
    }

    private RequeueAttempt rejectRequeueConcurrently(
            CountDownLatch startLatch,
            String requestId
    ) throws InterruptedException {
        awaitConcurrentStart(startLatch);

        try {
            repository.rejectManualReviewRequeueRequest(
                    requestId,
                    Instant.parse("2026-05-01T00:11:00Z"),
                    "ops-rejector",
                    "원인 조치 미확인"
            );
            return RequeueAttempt.success();
        } catch (RuntimeException exception) {
            return RequeueAttempt.failure(exception);
        }
    }

    private TransferAttempt applyConcurrentTransfer(
            CountDownLatch startLatch,
            String idempotencyKey,
            String fingerprint
    ) throws InterruptedException {
        awaitConcurrentStart(startLatch);

        try {
            repository.applyTransfer(
                    idempotencyKey,
                    fingerprint,
                    "wallet-001",
                    "wallet-002",
                    money("80000"),
                    "PostgreSQL 동시 송금",
                    Instant.parse("2026-05-01T00:00:00Z")
            );
            return TransferAttempt.success();
        } catch (RuntimeException exception) {
            return TransferAttempt.failure(exception);
        }
    }

    private ConsumerProcessedAttempt recordConsumerProcessedEventConcurrently(
            CountDownLatch startLatch,
            Instant processedAt
    ) throws InterruptedException {
        awaitConcurrentStart(startLatch);

        return new ConsumerProcessedAttempt(repository.recordProcessedEvent(
                "outbox-001",
                "outbox-001",
                "CHARGE_COMPLETED",
                processedAt
        ));
    }

    private ChargeAttempt applyConcurrentCharge(CountDownLatch startLatch) throws InterruptedException {
        awaitConcurrentStart(startLatch);

        try {
            WalletOperationOutcome outcome = repository.applyCharge(
                    "postgres-concurrent-charge-001",
                    "postgres-concurrent-charge-fingerprint-001",
                    "wallet-001",
                    money("5000"),
                    "PostgreSQL 동시 충전",
                    Instant.parse("2026-05-01T00:00:00Z")
            );
            return ChargeAttempt.success(outcome.record().result().operationId(), outcome.created());
        } catch (RuntimeException exception) {
            return ChargeAttempt.failure(exception);
        }
    }

    private void awaitConcurrentStart(CountDownLatch startLatch) throws InterruptedException {
        if (!startLatch.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for concurrent start");
        }
    }

    private void resetDatabase(DriverManagerDataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), "KRW");
    }

    private record TransferAttempt(boolean successful, RuntimeException exception) {

        private static TransferAttempt success() {
            return new TransferAttempt(true, null);
        }

        private static TransferAttempt failure(RuntimeException exception) {
            return new TransferAttempt(false, exception);
        }
    }

    private record RequeueAttempt(boolean successful, RuntimeException exception) {

        private static RequeueAttempt success() {
            return new RequeueAttempt(true, null);
        }

        private static RequeueAttempt failure(RuntimeException exception) {
            return new RequeueAttempt(false, exception);
        }
    }

    private record ConsumerProcessedAttempt(boolean recorded) {
    }

    private record ChargeAttempt(boolean successful, String operationId, boolean created, RuntimeException exception) {

        private static ChargeAttempt success(String operationId, boolean created) {
            return new ChargeAttempt(true, operationId, created, null);
        }

        private static ChargeAttempt failure(RuntimeException exception) {
            return new ChargeAttempt(false, null, false, exception);
        }
    }
}
