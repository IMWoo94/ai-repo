package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.InsufficientBalanceException;
import com.imwoo.airepo.wallet.application.WalletCommandRepository;
import com.imwoo.airepo.wallet.application.WalletConcurrencyException;
import com.imwoo.airepo.wallet.application.WalletLedgerQueryRepository;
import com.imwoo.airepo.wallet.application.WalletNotFoundException;
import com.imwoo.airepo.wallet.application.WalletOperationRecord;
import com.imwoo.airepo.wallet.application.WalletOperationResult;
import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.AuditEventType;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import com.imwoo.airepo.wallet.domain.Member;
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
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JDBC adapter for the wallet + ledger bounded context: member/account/balance queries,
 * transaction/ledger/audit history, and the charge/transfer command flows (balance locking,
 * step logs, operation persistence, outbox emission).
 */
final class JdbcWalletLedgerRepository implements WalletCommandRepository, WalletLedgerQueryRepository {

    private static final int LOCK_TIMEOUT_MILLIS = 1000;
    private static final String BUSY_BALANCE_MESSAGE = "Wallet balance is busy. Please retry.";

    private final WalletJdbcSupport support;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    JdbcWalletLedgerRepository(WalletJdbcSupport support) {
        this.support = support;
        this.jdbcTemplate = support.jdbc();
        this.transactionTemplate = support.transaction();
    }

    @Override
    public Optional<Member> findMember(String memberId) {
        return support.queryOptional(
                "select member_id, status, created_at from members where member_id = ?",
                support.memberMapper(),
                memberId
        );
    }

    @Override
    public Optional<WalletAccount> findWalletAccount(String walletId) {
        return support.queryOptional(
                "select wallet_id, member_id, status, created_at from wallet_accounts where wallet_id = ?",
                support.walletAccountMapper(),
                walletId
        );
    }

    @Override
    public Optional<WalletBalance> findBalance(String walletId) {
        return support.queryOptional(
                "select wallet_id, amount, currency, as_of from wallet_balances where wallet_id = ?",
                support.walletBalanceMapper(),
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
                support.transactionHistoryMapper(),
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
                support.ledgerEntryMapper(),
                walletId
        );
    }

    @Override
    public List<AuditEvent> findAuditEvents() {
        return jdbcTemplate.query(
                "select audit_event_id, operation_id, type, occurred_at, detail from audit_events",
                support.auditEventMapper()
        );
    }

    @Override
    public List<AuditEvent> findAuditEventsByWallet(String walletId) {
        return jdbcTemplate.query(
                """
                        select ae.audit_event_id, ae.operation_id, ae.type, ae.occurred_at, ae.detail
                        from audit_events ae
                        where ae.audit_event_id in (
                            select audit_event_id from audit_event_wallets where wallet_id = ?
                        )
                        """,
                support.auditEventMapper(),
                walletId
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
                support.operationStepLogMapper(),
                operationId
        );
    }

    @Override
    public List<OperationOutboxEvent> findOperationOutboxEvents(String operationId) {
        return jdbcTemplate.query(
                """
                        select outbox_event_id, operation_id, event_type, aggregate_type,
                               aggregate_id, payload, status, occurred_at,
                               attempt_count, next_retry_at, claimed_at, lease_expires_at,
                               published_at, last_error
                        from operation_outbox_events
                        where operation_id = ?
                        """,
                support.operationOutboxEventMapper(),
                operationId
        );
    }

    @Override
    public boolean existsOperationId(String operationId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from wallet_operations where operation_id = ?",
                Integer.class,
                operationId
        );
        return count != null && count > 0;
    }

    @Override
    public Optional<WalletOperationRecord> findOperation(String idempotencyKey) {
        return support.queryOptional(
                """
                        select idempotency_key, fingerprint, operation_id, transaction_id, wallet_id,
                               counterparty_wallet_id, occurred_at, type, status, direction, amount, currency,
                               balance_wallet_id, balance_amount, balance_currency, balance_as_of, description
                        from wallet_operations
                        where idempotency_key = ?
                        """,
                support.operationRecordMapper(),
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
            String operationId = support.nextId("op", "operation_id_seq");
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

            String transactionId = support.nextId("txn", "transaction_id_seq");
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
                    support.nextId("ledger", "ledger_entry_id_seq"),
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
                    support.nextId("audit", "audit_event_id_seq"),
                    operationId,
                    AuditEventType.CHARGE_COMPLETED,
                    occurredAt,
                    "Charge completed for wallet " + walletId,
                    List.of(walletId)
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

            String operationId = support.nextId("op", "operation_id_seq");
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

            String sourceTransactionId = support.nextId("txn", "transaction_id_seq");
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
                    support.nextId("txn", "transaction_id_seq"),
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
                    support.nextId("ledger", "ledger_entry_id_seq"),
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
                    support.nextId("ledger", "ledger_entry_id_seq"),
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
                    support.nextId("audit", "audit_event_id_seq"),
                    operationId,
                    AuditEventType.TRANSFER_COMPLETED,
                    occurredAt,
                    "Transfer completed from " + sourceWalletId + " to " + targetWalletId,
                    List.of(sourceWalletId, targetWalletId)
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
        return support.queryOptional(
                "select wallet_id, amount, currency, as_of from wallet_balances where wallet_id = ? for update",
                support.walletBalanceMapper(),
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
                support.walletBalanceMapper(),
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
                support.timestamp(walletBalance.asOf()),
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
                support.timestamp(occurredAt),
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
                support.timestamp(occurredAt),
                type.name(),
                direction.name(),
                money.amount(),
                money.currency(),
                balanceAfter.amount(),
                balanceAfter.currency(),
                description
        );
    }

    private void insertAuditEvent(
            String auditEventId,
            String operationId,
            AuditEventType type,
            Instant occurredAt,
            String detail,
            List<String> walletIds
    ) {
        jdbcTemplate.update(
                """
                        insert into audit_events (audit_event_id, operation_id, type, occurred_at, detail)
                        values (?, ?, ?, ?, ?)
                        """,
                auditEventId,
                operationId,
                type.name(),
                support.timestamp(occurredAt),
                detail
        );
        for (String walletId : walletIds) {
            jdbcTemplate.update(
                    """
                            insert into audit_event_wallets (audit_event_id, wallet_id)
                            values (?, ?)
                            """,
                    auditEventId,
                    walletId
            );
        }
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
                support.timestamp(result.occurredAt()),
                result.type().name(),
                result.status().name(),
                result.direction().name(),
                result.money().amount(),
                result.money().currency(),
                result.balance().walletId(),
                result.balance().money().amount(),
                result.balance().money().currency(),
                support.timestamp(result.balance().asOf()),
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
                support.nextId("step", "operation_step_log_id_seq"),
                operationId,
                step.name(),
                TransactionStatus.COMPLETED.name(),
                support.timestamp(occurredAt),
                detail
        );
    }

    private void insertOutboxEvent(WalletOperationResult result) {
        jdbcTemplate.update(
                """
                        insert into operation_outbox_events (
                            outbox_event_id, operation_id, event_type, aggregate_type,
                            aggregate_id, payload, status, occurred_at,
                            attempt_count, next_retry_at, claimed_at, lease_expires_at,
                            published_at, last_error
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                support.nextId("outbox", "outbox_event_id_seq"),
                result.operationId(),
                result.type().name() + "_COMPLETED",
                "WALLET_OPERATION",
                result.operationId(),
                outboxPayload(result),
                OperationOutboxStatus.PENDING.name(),
                support.timestamp(result.occurredAt()),
                0,
                null,
                null,
                null,
                null,
                null
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
}
