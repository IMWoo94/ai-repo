package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.Money;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.support.TransactionTemplate;

class JdbcWalletRepositoryRollbackTest {

    private EmbeddedDatabase database;
    private JdbcTemplate jdbcTemplate;
    private JdbcWalletRepository repository;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .setName("wallet;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE")
                .addScript("classpath:db/postgresql/schema.sql")
                .addScript("classpath:db/h2/fixtures.sql")
                .build();
        jdbcTemplate = new JdbcTemplate(database);
        repository = new JdbcWalletRepository(
                jdbcTemplate,
                new TransactionTemplate(new DataSourceTransactionManager(database))
        );
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void rollsBackEntireChargeTransactionWhenOperationInsertFails() {
        BigDecimal initialBalance = amount("wallet-001");
        Long initialTransactionCount = count("transaction_history where wallet_id = 'wallet-001'");
        Long initialLedgerCount = count("ledger_entries where wallet_id = 'wallet-001'");
        Long initialAuditCount = count("audit_events");

        insertPreexistingOperation("rollback-key");

        assertThatThrownBy(() -> repository.applyCharge(
                "rollback-key",
                "new-fingerprint",
                "wallet-001",
                new Money(new BigDecimal("5000"), "KRW"),
                "롤백 검증",
                Instant.parse("2026-05-01T00:00:00Z")
        )).isInstanceOf(DuplicateKeyException.class);

        assertThat(amount("wallet-001")).isEqualByComparingTo(initialBalance);
        assertThat(count("transaction_history where wallet_id = 'wallet-001'")).isEqualTo(initialTransactionCount);
        assertThat(count("ledger_entries where wallet_id = 'wallet-001'")).isEqualTo(initialLedgerCount);
        assertThat(count("audit_events")).isEqualTo(initialAuditCount);
    }

    private BigDecimal amount(String walletId) {
        return jdbcTemplate.queryForObject(
                "select amount from wallet_balances where wallet_id = ?",
                BigDecimal.class,
                walletId
        );
    }

    private Long count(String tableExpression) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableExpression, Long.class);
    }

    private void insertPreexistingOperation(String idempotencyKey) {
        jdbcTemplate.update(
                """
                        insert into wallet_operations (
                            idempotency_key, fingerprint, operation_id, transaction_id, wallet_id,
                            counterparty_wallet_id, occurred_at, type, status, direction, amount, currency,
                            balance_wallet_id, balance_amount, balance_currency, balance_as_of, description
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                idempotencyKey,
                "preexisting-fingerprint",
                "op-existing",
                "txn-existing",
                "wallet-001",
                null,
                Timestamp.from(Instant.parse("2026-05-01T00:00:00Z")),
                "CHARGE",
                "COMPLETED",
                "CREDIT",
                new BigDecimal("0"),
                "KRW",
                "wallet-001",
                new BigDecimal("125000"),
                "KRW",
                Timestamp.from(Instant.parse("2026-05-01T00:00:00Z")),
                "Pre-existing record"
        );
    }
}
