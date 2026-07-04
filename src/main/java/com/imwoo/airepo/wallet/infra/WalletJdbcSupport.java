package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.InvalidWalletOperationException;
import com.imwoo.airepo.wallet.domain.AdminApiAccessAudit;
import com.imwoo.airepo.wallet.domain.AdminApiAccessOutcome;
import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.AuditEventType;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.MemberStatus;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerProcessedEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueAudit;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueRequestRecord;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueRequestStatus;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRun;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRunStatus;
import com.imwoo.airepo.wallet.domain.OperationOutboxStatus;
import com.imwoo.airepo.wallet.domain.OperationStep;
import com.imwoo.airepo.wallet.domain.OperationStepLog;
import com.imwoo.airepo.wallet.domain.OperationalAlert;
import com.imwoo.airepo.wallet.domain.OperationalAlertSeverity;
import com.imwoo.airepo.wallet.domain.TransactionDirection;
import com.imwoo.airepo.wallet.domain.TransactionHistoryItem;
import com.imwoo.airepo.wallet.domain.TransactionStatus;
import com.imwoo.airepo.wallet.domain.TransactionType;
import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletAccountStatus;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import com.imwoo.airepo.wallet.application.WalletOperationRecord;
import com.imwoo.airepo.wallet.application.WalletOperationResult;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Shared JDBC infrastructure for the wallet persistence adapters.
 *
 * <p>Holds the {@link JdbcTemplate}/{@link TransactionTemplate} and centralises the
 * {@link RowMapper}s, id/timestamp helpers, and transaction patterns that each bounded-context
 * adapter reuses. Package-private on purpose: it is an implementation detail of the
 * {@code infra} package and never leaves it.
 */
final class WalletJdbcSupport {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    WalletJdbcSupport(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    JdbcTemplate jdbc() {
        return jdbcTemplate;
    }

    TransactionTemplate transaction() {
        return transactionTemplate;
    }

    String nextId(String prefix, String sequenceName) {
        Long nextValue = jdbcTemplate.queryForObject("select nextval('" + sequenceName + "')", Long.class);
        return "%s-%03d".formatted(prefix, nextValue);
    }

    String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    <T> Optional<T> queryOptional(String sql, RowMapper<T> rowMapper, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, args));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    void requireSingleRowUpdate(int updatedRows, String message) {
        if (updatedRows != 1) {
            throw new InvalidWalletOperationException(message);
        }
    }

    Timestamp timestamp(Instant instant) {
        if (instant == null) {
            return null;
        }
        return Timestamp.from(instant);
    }

    boolean isPostgresDatabase() {
        return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection ->
                "PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())));
    }

    Money money(ResultSet resultSet, String amountColumn, String currencyColumn) throws SQLException {
        BigDecimal amount = resultSet.getBigDecimal(amountColumn);
        String currency = resultSet.getString(currencyColumn);
        return new Money(amount, currency);
    }

    Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        return resultSet.getTimestamp(columnName).toInstant();
    }

    Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant();
    }

    RowMapper<Member> memberMapper() {
        return (resultSet, rowNumber) -> new Member(
                resultSet.getString("member_id"),
                MemberStatus.valueOf(resultSet.getString("status")),
                instant(resultSet, "created_at")
        );
    }

    RowMapper<WalletAccount> walletAccountMapper() {
        return (resultSet, rowNumber) -> new WalletAccount(
                resultSet.getString("wallet_id"),
                resultSet.getString("member_id"),
                WalletAccountStatus.valueOf(resultSet.getString("status")),
                instant(resultSet, "created_at")
        );
    }

    RowMapper<WalletBalance> walletBalanceMapper() {
        return (resultSet, rowNumber) -> new WalletBalance(
                resultSet.getString("wallet_id"),
                money(resultSet, "amount", "currency"),
                instant(resultSet, "as_of")
        );
    }

    RowMapper<TransactionHistoryItem> transactionHistoryMapper() {
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

    RowMapper<LedgerEntry> ledgerEntryMapper() {
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

    RowMapper<AuditEvent> auditEventMapper() {
        return (resultSet, rowNumber) -> new AuditEvent(
                resultSet.getString("audit_event_id"),
                resultSet.getString("operation_id"),
                AuditEventType.valueOf(resultSet.getString("type")),
                instant(resultSet, "occurred_at"),
                resultSet.getString("detail")
        );
    }

    RowMapper<OperationStepLog> operationStepLogMapper() {
        return (resultSet, rowNumber) -> new OperationStepLog(
                resultSet.getString("operation_step_log_id"),
                resultSet.getString("operation_id"),
                OperationStep.valueOf(resultSet.getString("step")),
                TransactionStatus.valueOf(resultSet.getString("status")),
                instant(resultSet, "occurred_at"),
                resultSet.getString("detail")
        );
    }

    RowMapper<OperationOutboxEvent> operationOutboxEventMapper() {
        return (resultSet, rowNumber) -> new OperationOutboxEvent(
                resultSet.getString("outbox_event_id"),
                resultSet.getString("operation_id"),
                resultSet.getString("event_type"),
                resultSet.getString("aggregate_type"),
                resultSet.getString("aggregate_id"),
                resultSet.getString("payload"),
                OperationOutboxStatus.valueOf(resultSet.getString("status")),
                instant(resultSet, "occurred_at"),
                resultSet.getInt("attempt_count"),
                nullableInstant(resultSet, "next_retry_at"),
                nullableInstant(resultSet, "claimed_at"),
                nullableInstant(resultSet, "lease_expires_at"),
                nullableInstant(resultSet, "published_at"),
                resultSet.getString("last_error")
        );
    }

    RowMapper<OperationOutboxRequeueAudit> outboxRequeueAuditMapper() {
        return (resultSet, rowNumber) -> new OperationOutboxRequeueAudit(
                resultSet.getString("audit_id"),
                resultSet.getString("outbox_event_id"),
                resultSet.getString("operation_id"),
                instant(resultSet, "requeued_at"),
                resultSet.getString("operator_name"),
                resultSet.getString("reason")
        );
    }

    RowMapper<OperationOutboxRequeueRequestRecord> outboxRequeueRequestMapper() {
        return (resultSet, rowNumber) -> new OperationOutboxRequeueRequestRecord(
                resultSet.getString("request_id"),
                resultSet.getString("outbox_event_id"),
                resultSet.getString("operation_id"),
                OperationOutboxRequeueRequestStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("requested_by"),
                resultSet.getString("request_reason"),
                instant(resultSet, "requested_at"),
                resultSet.getString("approved_by"),
                nullableInstant(resultSet, "approved_at"),
                resultSet.getString("approval_reason"),
                resultSet.getString("executed_by"),
                nullableInstant(resultSet, "executed_at"),
                resultSet.getString("rejected_by"),
                nullableInstant(resultSet, "rejected_at"),
                resultSet.getString("rejection_reason")
        );
    }

    RowMapper<OperationOutboxRelayRun> operationOutboxRelayRunMapper() {
        return (resultSet, rowNumber) -> new OperationOutboxRelayRun(
                resultSet.getString("relay_run_id"),
                instant(resultSet, "started_at"),
                instant(resultSet, "completed_at"),
                OperationOutboxRelayRunStatus.valueOf(resultSet.getString("status")),
                resultSet.getInt("batch_size"),
                resultSet.getInt("claimed_count"),
                resultSet.getInt("published_count"),
                resultSet.getInt("failed_count"),
                resultSet.getString("error_message")
        );
    }

    RowMapper<AdminApiAccessAudit> adminApiAccessAuditMapper() {
        return (resultSet, rowNumber) -> new AdminApiAccessAudit(
                resultSet.getString("audit_id"),
                instant(resultSet, "occurred_at"),
                resultSet.getString("method"),
                resultSet.getString("path"),
                resultSet.getString("operator_id"),
                resultSet.getInt("status_code"),
                AdminApiAccessOutcome.valueOf(resultSet.getString("outcome"))
        );
    }

    RowMapper<OperationalAlert> operationalAlertMapper() {
        return (resultSet, rowNumber) -> new OperationalAlert(
                resultSet.getString("alert_id"),
                resultSet.getString("source"),
                OperationalAlertSeverity.valueOf(resultSet.getString("severity")),
                instant(resultSet, "occurred_at"),
                java.util.List.of(resultSet.getString("reasons").split("\\n"))
        );
    }

    RowMapper<OperationOutboxConsumerProcessedEvent> operationOutboxConsumerProcessedEventMapper() {
        return (resultSet, rowNumber) -> new OperationOutboxConsumerProcessedEvent(
                resultSet.getString("idempotency_key"),
                resultSet.getString("outbox_event_id"),
                resultSet.getString("event_type"),
                instant(resultSet, "processed_at"),
                resultSet.getInt("duplicate_count")
        );
    }

    RowMapper<OperationOutboxConsumerReceipt> operationOutboxConsumerReceiptMapper() {
        return (resultSet, rowNumber) -> new OperationOutboxConsumerReceipt(
                resultSet.getString("idempotency_key"),
                resultSet.getString("outbox_event_id"),
                resultSet.getString("operation_id"),
                resultSet.getString("event_type"),
                resultSet.getString("aggregate_type"),
                resultSet.getString("aggregate_id"),
                instant(resultSet, "received_at")
        );
    }

    RowMapper<WalletOperationRecord> operationRecordMapper() {
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
}
