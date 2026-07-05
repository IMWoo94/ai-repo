package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.AdminApiAccessAuditRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerDeliveryMetricRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerIdempotencyRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerMetrics;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerMonitoringRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerPruningRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerReceiptRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerWindowMetrics;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayRunRepository;
import com.imwoo.airepo.wallet.application.OperationalAlertRepository;
import com.imwoo.airepo.wallet.application.WalletCommandRepository;
import com.imwoo.airepo.wallet.application.WalletLedgerQueryRepository;
import com.imwoo.airepo.wallet.application.WalletOperationRecord;
import com.imwoo.airepo.wallet.domain.AdminApiAccessAudit;
import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerProcessedEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueAudit;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueRequestRecord;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRun;
import com.imwoo.airepo.wallet.domain.OperationStepLog;
import com.imwoo.airepo.wallet.domain.OperationalAlert;
import com.imwoo.airepo.wallet.domain.OperationalAlertSeverity;
import com.imwoo.airepo.wallet.domain.TransactionHistoryItem;
import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PostgreSQL-backed composite of the wallet persistence ports.
 *
 * <p>This class no longer contains any SQL: it composes the bounded-context adapters
 * ({@link JdbcWalletLedgerRepository}, {@link JdbcOutboxRelayRepository},
 * {@link JdbcOutboxConsumerRepository}, {@link JdbcOperationalAlertRepository},
 * {@link JdbcAdminApiAccessAuditRepository}) over a shared {@link WalletJdbcSupport} and delegates
 * every port method. Retaining the aggregate bean under {@code @Profile("postgres")} keeps the
 * Spring wiring and the {@code (JdbcTemplate, TransactionTemplate)} construction contract identical
 * for callers and tests, while the SQL lives in focused, single-context adapters.
 *
 * <p>See ADR-0064 for the decomposition strategy and rationale.
 */
@Repository
@Profile("postgres")
public class JdbcWalletRepository implements
        WalletCommandRepository,
        WalletLedgerQueryRepository,
        OperationOutboxRelayRepository,
        OperationOutboxRelayRunRepository,
        OperationOutboxConsumerIdempotencyRepository,
        OperationOutboxConsumerReceiptRepository,
        OperationOutboxConsumerDeliveryMetricRepository,
        OperationOutboxConsumerMonitoringRepository,
        OperationOutboxConsumerPruningRepository,
        OperationalAlertRepository,
        AdminApiAccessAuditRepository {

    private final JdbcWalletLedgerRepository walletLedger;
    private final JdbcOutboxRelayRepository outboxRelay;
    private final JdbcOutboxConsumerRepository outboxConsumer;
    private final JdbcOperationalAlertRepository operationalAlert;
    private final JdbcAdminApiAccessAuditRepository adminApiAccessAudit;

    public JdbcWalletRepository(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        WalletJdbcSupport support = new WalletJdbcSupport(jdbcTemplate, transactionTemplate);
        this.walletLedger = new JdbcWalletLedgerRepository(support);
        this.outboxRelay = new JdbcOutboxRelayRepository(support);
        this.outboxConsumer = new JdbcOutboxConsumerRepository(support);
        this.operationalAlert = new JdbcOperationalAlertRepository(support);
        this.adminApiAccessAudit = new JdbcAdminApiAccessAuditRepository(support);
    }

    // --- WalletQueryRepository / WalletLedgerQueryRepository / WalletCommandRepository ---

    @Override
    public Optional<Member> findMember(String memberId) {
        return walletLedger.findMember(memberId);
    }

    @Override
    public Optional<WalletAccount> findWalletAccount(String walletId) {
        return walletLedger.findWalletAccount(walletId);
    }

    @Override
    public Optional<WalletBalance> findBalance(String walletId) {
        return walletLedger.findBalance(walletId);
    }

    @Override
    public List<TransactionHistoryItem> findTransactions(String walletId) {
        return walletLedger.findTransactions(walletId);
    }

    @Override
    public List<LedgerEntry> findLedgerEntries(String walletId) {
        return walletLedger.findLedgerEntries(walletId);
    }

    @Override
    public List<AuditEvent> findAuditEvents() {
        return walletLedger.findAuditEvents();
    }

    @Override
    public List<AuditEvent> findAuditEventsByWallet(String walletId) {
        return walletLedger.findAuditEventsByWallet(walletId);
    }

    @Override
    public List<OperationStepLog> findOperationStepLogs(String operationId) {
        return walletLedger.findOperationStepLogs(operationId);
    }

    @Override
    public List<OperationOutboxEvent> findOperationOutboxEvents(String operationId) {
        return walletLedger.findOperationOutboxEvents(operationId);
    }

    @Override
    public boolean existsOperationId(String operationId) {
        return walletLedger.existsOperationId(operationId);
    }

    @Override
    public Optional<WalletOperationRecord> findOperation(String walletId, String idempotencyKey) {
        return walletLedger.findOperation(walletId, idempotencyKey);
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
        return walletLedger.applyCharge(idempotencyKey, fingerprint, walletId, money, description, occurredAt);
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
        return walletLedger.applyTransfer(
                idempotencyKey, fingerprint, sourceWalletId, targetWalletId, money, description, occurredAt);
    }

    // --- OperationOutboxRelayRepository / OperationOutboxRelayRunRepository ---

    @Override
    public List<OperationOutboxEvent> findPendingOutboxEvents(int limit) {
        return outboxRelay.findPendingOutboxEvents(limit);
    }

    @Override
    public long countPendingOutboxEvents() {
        return outboxRelay.countPendingOutboxEvents();
    }

    @Override
    public List<OperationOutboxEvent> findManualReviewOutboxEvents(int limit) {
        return outboxRelay.findManualReviewOutboxEvents(limit);
    }

    @Override
    public List<OperationOutboxRequeueAudit> findOutboxRequeueAudits(String outboxEventId) {
        return outboxRelay.findOutboxRequeueAudits(outboxEventId);
    }

    @Override
    public List<OperationOutboxRequeueRequestRecord> findOutboxRequeueRequests(String outboxEventId) {
        return outboxRelay.findOutboxRequeueRequests(outboxEventId);
    }

    @Override
    public List<OperationOutboxEvent> claimReadyOutboxEvents(int limit, Instant now, Instant leaseExpiresAt) {
        return outboxRelay.claimReadyOutboxEvents(limit, now, leaseExpiresAt);
    }

    @Override
    public void markOutboxEventPublished(String outboxEventId, Instant publishedAt) {
        outboxRelay.markOutboxEventPublished(outboxEventId, publishedAt);
    }

    @Override
    public void markClaimedOutboxEventPublished(
            String outboxEventId,
            Instant claimedAt,
            Instant leaseExpiresAt,
            Instant publishedAt
    ) {
        outboxRelay.markClaimedOutboxEventPublished(outboxEventId, claimedAt, leaseExpiresAt, publishedAt);
    }

    @Override
    public void markOutboxEventFailed(String outboxEventId, String lastError, Instant nextRetryAt, int maxAttempts) {
        outboxRelay.markOutboxEventFailed(outboxEventId, lastError, nextRetryAt, maxAttempts);
    }

    @Override
    public void markClaimedOutboxEventFailed(
            String outboxEventId,
            Instant claimedAt,
            Instant leaseExpiresAt,
            String lastError,
            Instant nextRetryAt,
            int maxAttempts
    ) {
        outboxRelay.markClaimedOutboxEventFailed(
                outboxEventId, claimedAt, leaseExpiresAt, lastError, nextRetryAt, maxAttempts);
    }

    @Override
    public void requeueManualReviewOutboxEvent(
            String outboxEventId,
            Instant requeuedAt,
            String operator,
            String reason
    ) {
        outboxRelay.requeueManualReviewOutboxEvent(outboxEventId, requeuedAt, operator, reason);
    }

    @Override
    public OperationOutboxRequeueRequestRecord requestManualReviewRequeue(
            String outboxEventId,
            Instant requestedAt,
            String requestedBy,
            String reason
    ) {
        return outboxRelay.requestManualReviewRequeue(outboxEventId, requestedAt, requestedBy, reason);
    }

    @Override
    public OperationOutboxRequeueRequestRecord approveManualReviewRequeueRequest(
            String requestId,
            Instant approvedAt,
            String approvedBy,
            String approvalReason
    ) {
        return outboxRelay.approveManualReviewRequeueRequest(requestId, approvedAt, approvedBy, approvalReason);
    }

    @Override
    public OperationOutboxRequeueRequestRecord rejectManualReviewRequeueRequest(
            String requestId,
            Instant rejectedAt,
            String rejectedBy,
            String rejectionReason
    ) {
        return outboxRelay.rejectManualReviewRequeueRequest(requestId, rejectedAt, rejectedBy, rejectionReason);
    }

    @Override
    public OperationOutboxRequeueRequestRecord executeManualReviewRequeueRequest(
            String requestId,
            Instant executedAt,
            String executedBy
    ) {
        return outboxRelay.executeManualReviewRequeueRequest(requestId, executedAt, executedBy);
    }

    @Override
    public String nextRelayRunId() {
        return outboxRelay.nextRelayRunId();
    }

    @Override
    public void saveOutboxRelayRun(OperationOutboxRelayRun relayRun) {
        outboxRelay.saveOutboxRelayRun(relayRun);
    }

    @Override
    public List<OperationOutboxRelayRun> findRecentOutboxRelayRuns(int limit) {
        return outboxRelay.findRecentOutboxRelayRuns(limit);
    }

    @Override
    public int deleteOutboxRelayRunsCompletedBefore(Instant cutoff) {
        return outboxRelay.deleteOutboxRelayRunsCompletedBefore(cutoff);
    }

    // --- Outbox consumer (idempotency / receipt / delivery metric / monitoring / pruning) ---

    @Override
    public boolean recordProcessedEvent(
            String idempotencyKey,
            String outboxEventId,
            String eventType,
            Instant processedAt
    ) {
        return outboxConsumer.recordProcessedEvent(idempotencyKey, outboxEventId, eventType, processedAt);
    }

    @Override
    public Optional<OperationOutboxConsumerProcessedEvent> findProcessedEvent(String idempotencyKey) {
        return outboxConsumer.findProcessedEvent(idempotencyKey);
    }

    @Override
    public void saveConsumerReceipt(OperationOutboxConsumerReceipt receipt) {
        outboxConsumer.saveConsumerReceipt(receipt);
    }

    @Override
    public Optional<OperationOutboxConsumerReceipt> findConsumerReceipt(String idempotencyKey) {
        return outboxConsumer.findConsumerReceipt(idempotencyKey);
    }

    @Override
    public void recordConsumerDeliveryMetric(Instant occurredAt, boolean duplicate) {
        outboxConsumer.recordConsumerDeliveryMetric(occurredAt, duplicate);
    }

    @Override
    public OperationOutboxConsumerMetrics getConsumerMetrics() {
        return outboxConsumer.getConsumerMetrics();
    }

    @Override
    public OperationOutboxConsumerWindowMetrics getConsumerWindowMetrics(
            Instant windowStartedAt,
            Instant windowEndedAt
    ) {
        return outboxConsumer.getConsumerWindowMetrics(windowStartedAt, windowEndedAt);
    }

    @Override
    public List<OperationOutboxConsumerReceipt> findRecentConsumerReceipts(int limit) {
        return outboxConsumer.findRecentConsumerReceipts(limit);
    }

    @Override
    public int deleteConsumerProcessedEventsProcessedBefore(Instant cutoff) {
        return outboxConsumer.deleteConsumerProcessedEventsProcessedBefore(cutoff);
    }

    @Override
    public int deleteConsumerReceiptsReceivedBefore(Instant cutoff) {
        return outboxConsumer.deleteConsumerReceiptsReceivedBefore(cutoff);
    }

    @Override
    public int deleteConsumerDeliveryMetricsBucketStartedBefore(Instant cutoff) {
        return outboxConsumer.deleteConsumerDeliveryMetricsBucketStartedBefore(cutoff);
    }

    // --- OperationalAlertRepository ---

    @Override
    public String nextOperationalAlertId() {
        return operationalAlert.nextOperationalAlertId();
    }

    @Override
    public void saveOperationalAlert(OperationalAlert operationalAlertRecord) {
        operationalAlert.saveOperationalAlert(operationalAlertRecord);
    }

    @Override
    public boolean existsOperationalAlertBetween(
            String source,
            OperationalAlertSeverity severity,
            List<String> reasons,
            Instant since,
            Instant until
    ) {
        return operationalAlert.existsOperationalAlertBetween(source, severity, reasons, since, until);
    }

    @Override
    public List<OperationalAlert> findRecentOperationalAlerts(int limit) {
        return operationalAlert.findRecentOperationalAlerts(limit);
    }

    @Override
    public int deleteOperationalAlertsOccurredBefore(Instant cutoff) {
        return operationalAlert.deleteOperationalAlertsOccurredBefore(cutoff);
    }

    // --- AdminApiAccessAuditRepository ---

    @Override
    public String nextAdminApiAccessAuditId() {
        return adminApiAccessAudit.nextAdminApiAccessAuditId();
    }

    @Override
    public void saveAdminApiAccessAudit(AdminApiAccessAudit accessAudit) {
        adminApiAccessAudit.saveAdminApiAccessAudit(accessAudit);
    }

    @Override
    public List<AdminApiAccessAudit> findRecentAdminApiAccessAudits(int limit) {
        return adminApiAccessAudit.findRecentAdminApiAccessAudits(limit);
    }

    @Override
    public int deleteAdminApiAccessAuditsOccurredBefore(Instant cutoff) {
        return adminApiAccessAudit.deleteAdminApiAccessAuditsOccurredBefore(cutoff);
    }
}
