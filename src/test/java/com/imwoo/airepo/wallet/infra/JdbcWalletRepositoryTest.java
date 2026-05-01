package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.application.IdempotencyKeyConflictException;
import com.imwoo.airepo.wallet.application.InMemoryWalletCommandService;
import com.imwoo.airepo.wallet.application.InMemoryWalletLedgerQueryService;
import com.imwoo.airepo.wallet.application.WalletChargeCommand;
import com.imwoo.airepo.wallet.application.WalletCommandResult;
import com.imwoo.airepo.wallet.application.WalletTransferCommand;
import com.imwoo.airepo.wallet.domain.Money;
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
                    assertThat(outboxEvent.lastError()).isNull();
                });
        assertThat(repository.findPendingOutboxEvents(10)).isEmpty();
    }

    @Test
    void outboxRelayFailureIncrementsAttemptCount() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-db-001", "DB 충전"));

        repository.markOutboxEventFailed("outbox-001", "broker unavailable");

        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.FAILED);
                    assertThat(outboxEvent.attemptCount()).isEqualTo(1);
                    assertThat(outboxEvent.lastError()).isEqualTo("broker unavailable");
                    assertThat(outboxEvent.publishedAt()).isNull();
                });
        assertThat(repository.findPendingOutboxEvents(10)).isEmpty();
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
