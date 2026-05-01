package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.imwoo.airepo.wallet.application.InMemoryWalletCommandService;
import com.imwoo.airepo.wallet.application.InMemoryWalletLedgerQueryService;
import com.imwoo.airepo.wallet.application.WalletChargeCommand;
import com.imwoo.airepo.wallet.application.WalletCommandResult;
import com.imwoo.airepo.wallet.application.WalletTransferCommand;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.TransactionDirection;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        resetDatabase(jdbcTemplate);

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
    }

    private void resetDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute(readResource("/db/postgresql/schema.sql"));
        jdbcTemplate.execute("""
                truncate audit_events, ledger_entries, wallet_operations, transaction_history,
                         wallet_balances, wallet_accounts, members restart identity cascade
                """);
        jdbcTemplate.execute("alter sequence transaction_id_seq restart with 3");
        jdbcTemplate.execute("alter sequence operation_id_seq restart with 1");
        jdbcTemplate.execute("alter sequence ledger_entry_id_seq restart with 1");
        jdbcTemplate.execute("alter sequence audit_event_id_seq restart with 1");
        jdbcTemplate.execute(readResource("/db/postgresql/fixtures.sql"));
    }

    private String readResource(String path) {
        try {
            return new String(
                    Objects.requireNonNull(getClass().getResourceAsStream(path)).readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot read resource: " + path, exception);
        }
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), "KRW");
    }
}
