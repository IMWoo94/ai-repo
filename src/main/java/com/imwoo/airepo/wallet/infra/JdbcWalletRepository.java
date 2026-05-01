package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.InsufficientBalanceException;
import com.imwoo.airepo.wallet.application.WalletCommandRepository;
import com.imwoo.airepo.wallet.application.WalletConcurrencyException;
import com.imwoo.airepo.wallet.application.WalletLedgerQueryRepository;
import com.imwoo.airepo.wallet.application.WalletOperationRecord;
import com.imwoo.airepo.wallet.application.WalletOperationResult;
import com.imwoo.airepo.wallet.application.WalletNotFoundException;
import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.AuditEventType;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.MemberStatus;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxStatus;
import com.imwoo.airepo.wallet.domain.OperationStep;
import com.imwoo.airepo.wallet.domain.OperationStepLog;
import com.imwoo.airepo.wallet.domain.TransactionDirection;
import com.imwoo.airepo.wallet.domain.TransactionHistoryItem;
import com.imwoo.airepo.wallet.domain.TransactionStatus;
import com.imwoo.airepo.wallet.domain.TransactionType;
import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletAccountStatus;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@Profile("postgres")
public class JdbcWalletRepository implements WalletCommandRepository, WalletLedgerQueryRepository {

    private static final int LOCK_TIMEOUT_MILLIS = 1000;
    private static final String BUSY_BALANCE_MESSAGE = "Wallet balance is busy. Please retry.";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public JdbcWalletRepository(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Optional<Member> findMember(String memberId) {
        return queryOptional(
                "select member_id, status, created_at from members where member_id = ?",
                memberMapper(),
                memberId
        );
    }

    @Override
    public Optional<WalletAccount> findWalletAccount(String walletId) {
        return queryOptional(
                "select wallet_id, member_id, status, created_at from wallet_accounts where wallet_id = ?",
                walletAccountMapper(),
                walletId
        );
    }

    @Override
    public Optional<WalletBalance> findBalance(String walletId) {
        return queryOptional(
                "select wallet_id, amount, currency, as_of from wallet_balances where wallet_id = ?",
                walletBalanceMapper(),
                walletId
        );
    }

    @Override
    public List<TransactionHistoryItem> findTransactions(String walletId) {
        return jdbcTemplate.query(
                """
                        select transaction_id, wallet_id, occurred_at, type, status, direction, amount, currency, description
                        from transaction_history
                        where wallet_id = ?
                        """,
                transactionHistoryMapper(),
                walletId
        );
    }

    @Override
    public List<LedgerEntry> findLedgerEntries(String walletId) {
        return jdbcTemplate.query(
                """
                        select ledger_entry_id, operation_id, wallet_id, occurred_at, type, direction,
                               amount, currency, balance_after_amount, balance_after_currency, description
                        from ledger_entries
                        where wallet_id = ?
                        """,
                ledgerEntryMapper(),
                walletId
        );
    }

    @Override
    public List<AuditEvent> findAuditEvents() {
        return jdbcTemplate.query(
                "select audit_event_id, operation_id, type, occurred_at, detail from audit_events",
                auditEventMapper()
        );
    }

    @Override
    public List<OperationStepLog> findOperationStepLogs(String operationId) {
        return jdbcTemplate.query(
                """
                        select operation_step_log_id, operation_id, step, status, occurred_at, detail
                        from operation_step_logs
                        where operation_id = ?
                        """,
                operationStepLogMapper(),
                operationId
        );
    }

    @Override
    public List<OperationOutboxEvent> findOperationOutboxEvents(String operationId) {
        return jdbcTemplate.query(
                """
                        select outbox_event_id, operation_id, event_type, aggregate_type,
                               aggregate_id, payload, status, occurred_at
                        from operation_outbox_events
                        where operation_id = ?
                        """,
                operationOutboxEventMapper(),
                operationId
        );
    }

    @Override
    public Optional<WalletOperationRecord> findOperation(String idempotencyKey) {
        return queryOptional(
                """
                        select idempotency_key, fingerprint, operation_id, transaction_id, wallet_id,
                               counterparty_wallet_id, occurred_at, type, status, direction, amount, currency,
                               balance_wallet_id, balance_amount, balance_currency, balance_as_of, description
                        from wallet_operations
                        where idempotency_key = ?
                        """,
                operationRecordMapper(),
                idempotencyKey
        );
    }

    @Override
    public WalletOperationRecord applyCharge(
            String idempotencyKey,
            String fingerprint,
            String walletId,
            Money money,
            String description,
            Instant occurredAt
    ) {
        return executeWithLockTimeout(() -> {
            WalletBalance currentBalance = findBalanceForUpdate(walletId);
            WalletBalance updatedBalance = new WalletBalance(walletId, currentBalance.money().add(money), occurredAt);
            String operationId = nextId("op", "operation_id_seq");
            insertOperationStepLog(
                    operationId,
                    OperationStep.BALANCE_LOCKED,
                    occurredAt,
                    "Balance locked for wallet " + walletId
            );
            updateBalance(updatedBalance);
            insertOperationStepLog(
                    operationId,
                    OperationStep.BALANCE_UPDATED,
                    occurredAt,
                    "Balance updated for wallet " + walletId
            );

            String transactionId = nextId("txn", "transaction_id_seq");
            insertTransaction(
                    transactionId,
                    walletId,
                    occurredAt,
                    TransactionType.CHARGE,
                    TransactionStatus.COMPLETED,
                    TransactionDirection.CREDIT,
                    money,
                    description
            );
            insertOperationStepLog(
                    operationId,
                    OperationStep.TRANSACTION_RECORDED,
                    occurredAt,
                    "Transaction history recorded for wallet " + walletId
            );
            insertLedgerEntry(
                    nextId("ledger", "ledger_entry_id_seq"),
                    operationId,
                    walletId,
                    occurredAt,
                    TransactionType.CHARGE,
                    TransactionDirection.CREDIT,
                    money,
                    updatedBalance.money(),
                    description
            );
            insertOperationStepLog(
                    operationId,
                    OperationStep.LEDGER_RECORDED,
                    occurredAt,
                    "Ledger entry recorded for wallet " + walletId
            );
            insertAuditEvent(
                    nextId("audit", "audit_event_id_seq"),
                    operationId,
                    AuditEventType.CHARGE_COMPLETED,
                    occurredAt,
                    "Charge completed for wallet " + walletId
            );
            insertOperationStepLog(
                    operationId,
                    OperationStep.AUDIT_RECORDED,
                    occurredAt,
                    "Audit event recorded for operation " + operationId
            );

            WalletOperationResult result = new WalletOperationResult(
                    operationId,
                    transactionId,
                    walletId,
                    null,
                    occurredAt,
                    TransactionType.CHARGE,
                    TransactionStatus.COMPLETED,
                    TransactionDirection.CREDIT,
                    money,
                    updatedBalance,
                    description
            );
            WalletOperationRecord record = new WalletOperationRecord(idempotencyKey, fingerprint, result);
            insertOperation(record);
            insertOperationStepLog(
                    operationId,
                    OperationStep.IDEMPOTENCY_RECORDED,
                    occurredAt,
                    "Idempotency record stored for operation " + operationId
            );
            insertOutboxEvent(result);
            return record;
        });
    }

    @Override
    public WalletOperationRecord applyTransfer(
            String idempotencyKey,
            String fingerprint,
            String sourceWalletId,
            String targetWalletId,
            Money money,
            String description,
            Instant occurredAt
    ) {
        return executeWithLockTimeout(() -> {
            List<WalletBalance> lockedBalances = findTransferBalancesForUpdate(sourceWalletId, targetWalletId);
            WalletBalance sourceBalance = findLockedBalance(lockedBalances, sourceWalletId);
            WalletBalance targetBalance = findLockedBalance(lockedBalances, targetWalletId);
            if (sourceBalance.money().lessThan(money)) {
                throw new InsufficientBalanceException(sourceWalletId);
            }

            String operationId = nextId("op", "operation_id_seq");
            insertOperationStepLog(
                    operationId,
                    OperationStep.BALANCE_LOCKED,
                    occurredAt,
                    "Balances locked for transfer " + sourceWalletId + " to " + targetWalletId
            );
            WalletBalance updatedSourceBalance = new WalletBalance(
                    sourceWalletId,
                    sourceBalance.money().subtract(money),
                    occurredAt
            );
            WalletBalance updatedTargetBalance = new WalletBalance(
                    targetWalletId,
                    targetBalance.money().add(money),
                    occurredAt
            );
            updateBalance(updatedSourceBalance);
            updateBalance(updatedTargetBalance);
            insertOperationStepLog(
                    operationId,
                    OperationStep.BALANCE_UPDATED,
                    occurredAt,
                    "Balances updated for transfer " + sourceWalletId + " to " + targetWalletId
            );

            String sourceTransactionId = nextId("txn", "transaction_id_seq");
            insertTransaction(
                    sourceTransactionId,
                    sourceWalletId,
                    occurredAt,
                    TransactionType.TRANSFER,
                    TransactionStatus.COMPLETED,
                    TransactionDirection.DEBIT,
                    money,
                    description
            );
            insertTransaction(
                    nextId("txn", "transaction_id_seq"),
                    targetWalletId,
                    occurredAt,
                    TransactionType.TRANSFER,
                    TransactionStatus.COMPLETED,
                    TransactionDirection.CREDIT,
                    money,
                    description
            );
            insertOperationStepLog(
                    operationId,
                    OperationStep.TRANSACTION_RECORDED,
                    occurredAt,
                    "Transaction history recorded for transfer " + sourceWalletId + " to " + targetWalletId
            );
            insertLedgerEntry(
                    nextId("ledger", "ledger_entry_id_seq"),
                    operationId,
                    sourceWalletId,
                    occurredAt,
                    TransactionType.TRANSFER,
                    TransactionDirection.DEBIT,
                    money,
                    updatedSourceBalance.money(),
                    description
            );
            insertLedgerEntry(
                    nextId("ledger", "ledger_entry_id_seq"),
                    operationId,
                    targetWalletId,
                    occurredAt,
                    TransactionType.TRANSFER,
                    TransactionDirection.CREDIT,
                    money,
                    updatedTargetBalance.money(),
                    description
            );
            insertOperationStepLog(
                    operationId,
                    OperationStep.LEDGER_RECORDED,
                    occurredAt,
                    "Ledger entries recorded for transfer " + sourceWalletId + " to " + targetWalletId
            );
            insertAuditEvent(
                    nextId("audit", "audit_event_id_seq"),
                    operationId,
                    AuditEventType.TRANSFER_COMPLETED,
                    occurredAt,
                    "Transfer completed from " + sourceWalletId + " to " + targetWalletId
            );
            insertOperationStepLog(
                    operationId,
                    OperationStep.AUDIT_RECORDED,
                    occurredAt,
                    "Audit event recorded for operation " + operationId
            );

            WalletOperationResult result = new WalletOperationResult(
                    operationId,
                    sourceTransactionId,
                    sourceWalletId,
                    targetWalletId,
                    occurredAt,
                    TransactionType.TRANSFER,
                    TransactionStatus.COMPLETED,
                    TransactionDirection.DEBIT,
                    money,
                    updatedSourceBalance,
                    description
            );
            WalletOperationRecord record = new WalletOperationRecord(idempotencyKey, fingerprint, result);
            insertOperation(record);
            insertOperationStepLog(
                    operationId,
                    OperationStep.IDEMPOTENCY_RECORDED,
                    occurredAt,
                    "Idempotency record stored for operation " + operationId
            );
            insertOutboxEvent(result);
            return record;
        });
    }

    private WalletOperationRecord executeWithLockTimeout(Supplier<WalletOperationRecord> operation) {
        try {
            return transactionTemplate.execute(status -> {
                applyLockTimeout();
                return operation.get();
            });
        } catch (DataAccessException exception) {
            if (!causedByLockTimeout(exception)) {
                throw exception;
            }
            throw new WalletConcurrencyException(BUSY_BALANCE_MESSAGE, exception);
        }
    }

    private boolean causedByLockTimeout(DataAccessException exception) {
        if (exception instanceof CannotAcquireLockException) {
            return true;
        }

        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException && "55P03".equals(sqlException.getSQLState())) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

    private void applyLockTimeout() {
        try {
            jdbcTemplate.execute("set local lock_timeout = '" + LOCK_TIMEOUT_MILLIS + "ms'");
        } catch (BadSqlGrammarException exception) {
            jdbcTemplate.execute("set lock_timeout " + LOCK_TIMEOUT_MILLIS);
        }
    }

    private WalletBalance findBalanceForUpdate(String walletId) {
        return queryOptional(
                "select wallet_id, amount, currency, as_of from wallet_balances where wallet_id = ? for update",
                walletBalanceMapper(),
                walletId
        )
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    private List<WalletBalance> findTransferBalancesForUpdate(String sourceWalletId, String targetWalletId) {
        List<String> walletIds = List.of(sourceWalletId, targetWalletId).stream()
                .sorted()
                .toList();

        return jdbcTemplate.query(
                """
                        select wallet_id, amount, currency, as_of
                        from wallet_balances
                        where wallet_id in (?, ?)
                        order by wallet_id
                        for update
                        """,
                walletBalanceMapper(),
                walletIds.get(0),
                walletIds.get(1)
        );
    }

    private WalletBalance findLockedBalance(List<WalletBalance> lockedBalances, String walletId) {
        return lockedBalances.stream()
                .filter(walletBalance -> walletBalance.walletId().equals(walletId))
                .findFirst()
                .orElseThrow(() -> new WalletNotFoundException(walletId));
    }

    private void updateBalance(WalletBalance walletBalance) {
        jdbcTemplate.update(
                "update wallet_balances set amount = ?, currency = ?, as_of = ? where wallet_id = ?",
                walletBalance.money().amount(),
                walletBalance.money().currency(),
                timestamp(walletBalance.asOf()),
                walletBalance.walletId()
        );
    }

    private void insertTransaction(
            String transactionId,
            String walletId,
            Instant occurredAt,
            TransactionType type,
            TransactionStatus status,
            TransactionDirection direction,
            Money money,
            String description
    ) {
        jdbcTemplate.update(
                """
                        insert into transaction_history (
                            transaction_id, wallet_id, occurred_at, type, status, direction, amount, currency, description
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                transactionId,
                walletId,
                timestamp(occurredAt),
                type.name(),
                status.name(),
                direction.name(),
                money.amount(),
                money.currency(),
                description
        );
    }

    private void insertLedgerEntry(
            String ledgerEntryId,
            String operationId,
            String walletId,
            Instant occurredAt,
            TransactionType type,
            TransactionDirection direction,
            Money money,
            Money balanceAfter,
            String description
    ) {
        jdbcTemplate.update(
                """
                        insert into ledger_entries (
                            ledger_entry_id, operation_id, wallet_id, occurred_at, type, direction,
                            amount, currency, balance_after_amount, balance_after_currency, description
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                ledgerEntryId,
                operationId,
                walletId,
                timestamp(occurredAt),
                type.name(),
                direction.name(),
                money.amount(),
                money.currency(),
                balanceAfter.amount(),
                balanceAfter.currency(),
                description
        );
    }

    private void insertAuditEvent(String auditEventId, String operationId, AuditEventType type, Instant occurredAt, String detail) {
        jdbcTemplate.update(
                """
                        insert into audit_events (audit_event_id, operation_id, type, occurred_at, detail)
                        values (?, ?, ?, ?, ?)
                        """,
                auditEventId,
                operationId,
                type.name(),
                timestamp(occurredAt),
                detail
        );
    }

    private void insertOperation(WalletOperationRecord record) {
        WalletOperationResult result = record.result();
        jdbcTemplate.update(
                """
                        insert into wallet_operations (
                            idempotency_key, fingerprint, operation_id, transaction_id, wallet_id, counterparty_wallet_id,
                            occurred_at, type, status, direction, amount, currency, balance_wallet_id,
                            balance_amount, balance_currency, balance_as_of, description
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                record.idempotencyKey(),
                record.fingerprint(),
                result.operationId(),
                result.transactionId(),
                result.walletId(),
                result.counterpartyWalletId(),
                timestamp(result.occurredAt()),
                result.type().name(),
                result.status().name(),
                result.direction().name(),
                result.money().amount(),
                result.money().currency(),
                result.balance().walletId(),
                result.balance().money().amount(),
                result.balance().money().currency(),
                timestamp(result.balance().asOf()),
                result.description()
        );
    }

    private void insertOperationStepLog(
            String operationId,
            OperationStep step,
            Instant occurredAt,
            String detail
    ) {
        jdbcTemplate.update(
                """
                        insert into operation_step_logs (
                            operation_step_log_id, operation_id, step, status, occurred_at, detail
                        )
                        values (?, ?, ?, ?, ?, ?)
                        """,
                nextId("step", "operation_step_log_id_seq"),
                operationId,
                step.name(),
                TransactionStatus.COMPLETED.name(),
                timestamp(occurredAt),
                detail
        );
    }

    private void insertOutboxEvent(WalletOperationResult result) {
        jdbcTemplate.update(
                """
                        insert into operation_outbox_events (
                            outbox_event_id, operation_id, event_type, aggregate_type,
                            aggregate_id, payload, status, occurred_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                nextId("outbox", "outbox_event_id_seq"),
                result.operationId(),
                result.type().name() + "_COMPLETED",
                "WALLET_OPERATION",
                result.operationId(),
                outboxPayload(result),
                OperationOutboxStatus.PENDING.name(),
                timestamp(result.occurredAt())
        );
    }

    private String outboxPayload(WalletOperationResult result) {
        return """
                {"operationId":"%s","walletId":"%s","counterpartyWalletId":%s,"type":"%s","amount":"%s","currency":"%s"}
                """.formatted(
                result.operationId(),
                result.walletId(),
                nullableJsonString(result.counterpartyWalletId()),
                result.type().name(),
                result.money().amount().stripTrailingZeros().toPlainString(),
                result.money().currency()
        ).trim();
    }

    private String nullableJsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value + "\"";
    }

    private String nextId(String prefix, String sequenceName) {
        Long nextValue = jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Long.class);
        return "%s-%03d".formatted(prefix, nextValue);
    }

    private <T> Optional<T> queryOptional(String sql, RowMapper<T> rowMapper, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, args));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    private RowMapper<Member> memberMapper() {
        return (resultSet, rowNumber) -> new Member(
                resultSet.getString("member_id"),
                MemberStatus.valueOf(resultSet.getString("status")),
                instant(resultSet, "created_at")
        );
    }

    private RowMapper<WalletAccount> walletAccountMapper() {
        return (resultSet, rowNumber) -> new WalletAccount(
                resultSet.getString("wallet_id"),
                resultSet.getString("member_id"),
                WalletAccountStatus.valueOf(resultSet.getString("status")),
                instant(resultSet, "created_at")
        );
    }

    private RowMapper<WalletBalance> walletBalanceMapper() {
        return (resultSet, rowNumber) -> new WalletBalance(
                resultSet.getString("wallet_id"),
                money(resultSet, "amount", "currency"),
                instant(resultSet, "as_of")
        );
    }

    private RowMapper<TransactionHistoryItem> transactionHistoryMapper() {
        return (resultSet, rowNumber) -> new TransactionHistoryItem(
                resultSet.getString("transaction_id"),
                resultSet.getString("wallet_id"),
                instant(resultSet, "occurred_at"),
                TransactionType.valueOf(resultSet.getString("type")),
                TransactionStatus.valueOf(resultSet.getString("status")),
                TransactionDirection.valueOf(resultSet.getString("direction")),
                money(resultSet, "amount", "currency"),
                resultSet.getString("description")
        );
    }

    private RowMapper<LedgerEntry> ledgerEntryMapper() {
        return (resultSet, rowNumber) -> new LedgerEntry(
                resultSet.getString("ledger_entry_id"),
                resultSet.getString("operation_id"),
                resultSet.getString("wallet_id"),
                instant(resultSet, "occurred_at"),
                TransactionType.valueOf(resultSet.getString("type")),
                TransactionDirection.valueOf(resultSet.getString("direction")),
                money(resultSet, "amount", "currency"),
                money(resultSet, "balance_after_amount", "balance_after_currency"),
                resultSet.getString("description")
        );
    }

    private RowMapper<AuditEvent> auditEventMapper() {
        return (resultSet, rowNumber) -> new AuditEvent(
                resultSet.getString("audit_event_id"),
                resultSet.getString("operation_id"),
                AuditEventType.valueOf(resultSet.getString("type")),
                instant(resultSet, "occurred_at"),
                resultSet.getString("detail")
        );
    }

    private RowMapper<OperationStepLog> operationStepLogMapper() {
        return (resultSet, rowNumber) -> new OperationStepLog(
                resultSet.getString("operation_step_log_id"),
                resultSet.getString("operation_id"),
                OperationStep.valueOf(resultSet.getString("step")),
                TransactionStatus.valueOf(resultSet.getString("status")),
                instant(resultSet, "occurred_at"),
                resultSet.getString("detail")
        );
    }

    private RowMapper<OperationOutboxEvent> operationOutboxEventMapper() {
        return (resultSet, rowNumber) -> new OperationOutboxEvent(
                resultSet.getString("outbox_event_id"),
                resultSet.getString("operation_id"),
                resultSet.getString("event_type"),
                resultSet.getString("aggregate_type"),
                resultSet.getString("aggregate_id"),
                resultSet.getString("payload"),
                OperationOutboxStatus.valueOf(resultSet.getString("status")),
                instant(resultSet, "occurred_at")
        );
    }

    private RowMapper<WalletOperationRecord> operationRecordMapper() {
        return (resultSet, rowNumber) -> new WalletOperationRecord(
                resultSet.getString("idempotency_key"),
                resultSet.getString("fingerprint"),
                new WalletOperationResult(
                        resultSet.getString("operation_id"),
                        resultSet.getString("transaction_id"),
                        resultSet.getString("wallet_id"),
                        resultSet.getString("counterparty_wallet_id"),
                        instant(resultSet, "occurred_at"),
                        TransactionType.valueOf(resultSet.getString("type")),
                        TransactionStatus.valueOf(resultSet.getString("status")),
                        TransactionDirection.valueOf(resultSet.getString("direction")),
                        money(resultSet, "amount", "currency"),
                        new WalletBalance(
                                resultSet.getString("balance_wallet_id"),
                                money(resultSet, "balance_amount", "balance_currency"),
                                instant(resultSet, "balance_as_of")
                        ),
                        resultSet.getString("description")
                )
        );
    }

    private Money money(ResultSet resultSet, String amountColumn, String currencyColumn) throws SQLException {
        BigDecimal amount = resultSet.getBigDecimal(amountColumn);
        String currency = resultSet.getString(currencyColumn);
        return new Money(amount, currency);
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }

    private Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getTimestamp(columnName).toInstant();
    }
}
