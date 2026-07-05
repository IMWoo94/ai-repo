package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.InvalidWalletOperationException;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayRunRepository;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRun;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueAudit;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueRequestRecord;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueRequestStatus;
import com.imwoo.airepo.wallet.domain.OperationOutboxStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * JDBC adapter for the outbox relay bounded context: pending/claim/publish/fail transitions,
 * manual-review requeue workflow (direct + request/approve/reject/execute), and relay-run history.
 */
final class JdbcOutboxRelayRepository implements OperationOutboxRelayRepository, OperationOutboxRelayRunRepository {

    private static final int MAX_LAST_ERROR_LENGTH = 255;

    private final WalletJdbcSupport support;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    JdbcOutboxRelayRepository(WalletJdbcSupport support) {
        this.support = support;
        this.jdbcTemplate = support.jdbc();
        this.transactionTemplate = support.transaction();
    }

    @Override
    public List<OperationOutboxEvent> findPendingOutboxEvents(int limit) {
        return jdbcTemplate.query(
                """
                        select outbox_event_id, operation_id, event_type, aggregate_type,
                               aggregate_id, payload, status, occurred_at,
                               attempt_count, next_retry_at, claimed_at, lease_expires_at,
                               published_at, last_error
                        from operation_outbox_events
                        where status = ?
                        order by occurred_at, outbox_event_id
                        limit ?
                        """,
                support.operationOutboxEventMapper(),
                OperationOutboxStatus.PENDING.name(),
                limit
        );
    }

    @Override
    public long countPendingOutboxEvents() {
        Long count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from operation_outbox_events
                        where status = ?
                        """,
                Long.class,
                OperationOutboxStatus.PENDING.name()
        );
        return count == null ? 0L : count;
    }

    @Override
    public List<OperationOutboxEvent> findManualReviewOutboxEvents(int limit) {
        return jdbcTemplate.query(
                """
                        select outbox_event_id, operation_id, event_type, aggregate_type,
                               aggregate_id, payload, status, occurred_at,
                               attempt_count, next_retry_at, claimed_at, lease_expires_at,
                               published_at, last_error
                        from operation_outbox_events
                        where status = ?
                        order by occurred_at, outbox_event_id
                        limit ?
                        """,
                support.operationOutboxEventMapper(),
                OperationOutboxStatus.MANUAL_REVIEW.name(),
                limit
        );
    }

    @Override
    public List<OperationOutboxRequeueAudit> findOutboxRequeueAudits(String outboxEventId) {
        return jdbcTemplate.query(
                """
                        select audit_id, outbox_event_id, operation_id, requeued_at, operator_name, reason
                        from operation_outbox_requeue_audits
                        where outbox_event_id = ?
                        order by requeued_at, audit_id
                        """,
                support.outboxRequeueAuditMapper(),
                outboxEventId
        );
    }

    @Override
    public List<OperationOutboxRequeueRequestRecord> findOutboxRequeueRequests(String outboxEventId) {
        return jdbcTemplate.query(
                """
                        select request_id, outbox_event_id, operation_id, status, requested_by,
                               request_reason, requested_at, approved_by, approved_at, approval_reason,
                               executed_by, executed_at, rejected_by, rejected_at, rejection_reason
                        from operation_outbox_requeue_requests
                        where outbox_event_id = ?
                        order by requested_at, request_id
                        """,
                support.outboxRequeueRequestMapper(),
                outboxEventId
        );
    }

    @Override
    public List<OperationOutboxEvent> claimReadyOutboxEvents(int limit, Instant now, Instant leaseExpiresAt) {
        return transactionTemplate.execute(status -> {
            List<String> outboxEventIds = claimReadyOutboxEventIds(limit, now);
            if (outboxEventIds.isEmpty()) {
                return List.of();
            }
            for (String outboxEventId : outboxEventIds) {
                jdbcTemplate.update(
                        """
                                update operation_outbox_events
                                set status = ?, next_retry_at = null, claimed_at = ?,
                                    lease_expires_at = ?, published_at = null, last_error = null
                                where outbox_event_id = ?
                                """,
                        OperationOutboxStatus.PROCESSING.name(),
                        support.timestamp(now),
                        support.timestamp(leaseExpiresAt),
                        outboxEventId
                );
            }
            return jdbcTemplate.query(
                    """
                            select outbox_event_id, operation_id, event_type, aggregate_type,
                                   aggregate_id, payload, status, occurred_at,
                                   attempt_count, next_retry_at, claimed_at, lease_expires_at,
                                   published_at, last_error
                            from operation_outbox_events
                            where outbox_event_id in (%s)
                            order by occurred_at, outbox_event_id
                            """.formatted(support.placeholders(outboxEventIds.size())),
                    support.operationOutboxEventMapper(),
                    outboxEventIds.toArray()
            );
        });
    }

    @Override
    public void markOutboxEventPublished(String outboxEventId, Instant publishedAt) {
        jdbcTemplate.update(
                """
                        update operation_outbox_events
                        set status = ?, next_retry_at = null, claimed_at = null,
                            lease_expires_at = null, published_at = ?, last_error = null
                        where outbox_event_id = ?
                        """,
                OperationOutboxStatus.PUBLISHED.name(),
                support.timestamp(publishedAt),
                outboxEventId
        );
    }

    @Override
    public void markClaimedOutboxEventPublished(
            String outboxEventId,
            Instant claimedAt,
            Instant leaseExpiresAt,
            Instant publishedAt
    ) {
        support.requireSingleRowUpdate(
                jdbcTemplate.update(
                        """
                                update operation_outbox_events
                                set status = ?, next_retry_at = null, claimed_at = null,
                                    lease_expires_at = null, published_at = ?, last_error = null
                                where outbox_event_id = ?
                                  and status = ?
                                  and claimed_at = ?
                                  and lease_expires_at = ?
                                """,
                        OperationOutboxStatus.PUBLISHED.name(),
                        support.timestamp(publishedAt),
                        outboxEventId,
                        OperationOutboxStatus.PROCESSING.name(),
                        support.timestamp(claimedAt),
                        support.timestamp(leaseExpiresAt)
                ),
                "outbox event claim is no longer active: " + outboxEventId
        );
    }

    @Override
    public void markOutboxEventFailed(String outboxEventId, String lastError, Instant nextRetryAt, int maxAttempts) {
        jdbcTemplate.update(
                """
                        update operation_outbox_events
                        set status = case
                                when attempt_count + 1 >= ? then ?
                                else ?
                            end,
                            attempt_count = attempt_count + 1,
                            next_retry_at = case
                                when attempt_count + 1 >= ? then null
                                else cast(? as timestamp)
                            end,
                            claimed_at = null, lease_expires_at = null, published_at = null, last_error = ?
                        where outbox_event_id = ?
                        """,
                maxAttempts,
                OperationOutboxStatus.MANUAL_REVIEW.name(),
                OperationOutboxStatus.FAILED.name(),
                maxAttempts,
                support.timestamp(nextRetryAt),
                truncateLastError(lastError),
                outboxEventId
        );
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
        support.requireSingleRowUpdate(
                jdbcTemplate.update(
                        """
                                update operation_outbox_events
                                set status = case
                                        when attempt_count + 1 >= ? then ?
                                        else ?
                                    end,
                                    attempt_count = attempt_count + 1,
                                    next_retry_at = case
                                        when attempt_count + 1 >= ? then null
                                        else cast(? as timestamp)
                                    end,
                                    claimed_at = null, lease_expires_at = null, published_at = null, last_error = ?
                                where outbox_event_id = ?
                                  and status = ?
                                  and claimed_at = ?
                                  and lease_expires_at = ?
                                """,
                        maxAttempts,
                        OperationOutboxStatus.MANUAL_REVIEW.name(),
                        OperationOutboxStatus.FAILED.name(),
                        maxAttempts,
                        support.timestamp(nextRetryAt),
                        truncateLastError(lastError),
                        outboxEventId,
                        OperationOutboxStatus.PROCESSING.name(),
                        support.timestamp(claimedAt),
                        support.timestamp(leaseExpiresAt)
                ),
                "outbox event claim is no longer active: " + outboxEventId
        );
    }

    @Override
    public void requeueManualReviewOutboxEvent(
            String outboxEventId,
            Instant requeuedAt,
            String operator,
            String reason
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            OperationOutboxEvent event = manualReviewOutboxEventForUpdate(outboxEventId);

            support.requireSingleRowUpdate(
                    jdbcTemplate.update(
                            """
                                    update operation_outbox_events
                                    set status = ?, attempt_count = 0, next_retry_at = null,
                                        claimed_at = null, lease_expires_at = null, published_at = null, last_error = null
                                    where outbox_event_id = ?
                                      and status = ?
                                    """,
                            OperationOutboxStatus.PENDING.name(),
                            outboxEventId,
                            OperationOutboxStatus.MANUAL_REVIEW.name()
                    ),
                    "outbox event must still be MANUAL_REVIEW: " + outboxEventId
            );
            jdbcTemplate.update(
                    """
                            insert into operation_outbox_requeue_audits (
                                audit_id, outbox_event_id, operation_id, requeued_at, operator_name, reason
                            )
                            values (?, ?, ?, ?, ?, ?)
                            """,
                    support.nextId("outbox-requeue-audit", "outbox_requeue_audit_id_seq"),
                    outboxEventId,
                    event.operationId(),
                    support.timestamp(requeuedAt),
                    operator,
                    reason
            );
        });
    }

    @Override
    public OperationOutboxRequeueRequestRecord requestManualReviewRequeue(
            String outboxEventId,
            Instant requestedAt,
            String requestedBy,
            String reason
    ) {
        return transactionTemplate.execute(status -> {
            OperationOutboxEvent event = manualReviewOutboxEvent(outboxEventId);
            OperationOutboxRequeueRequestRecord request = new OperationOutboxRequeueRequestRecord(
                    support.nextId("outbox-requeue-request", "outbox_requeue_request_id_seq"),
                    outboxEventId,
                    event.operationId(),
                    OperationOutboxRequeueRequestStatus.REQUESTED,
                    requestedBy,
                    reason,
                    requestedAt,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            jdbcTemplate.update(
                    """
                            insert into operation_outbox_requeue_requests (
                                request_id, outbox_event_id, operation_id, status, requested_by,
                                request_reason, requested_at, approved_by, approved_at, approval_reason,
                                executed_by, executed_at, rejected_by, rejected_at, rejection_reason
                            )
                            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    request.requestId(),
                    request.outboxEventId(),
                    request.operationId(),
                    request.status().name(),
                    request.requestedBy(),
                    request.requestReason(),
                    support.timestamp(request.requestedAt()),
                    request.approvedBy(),
                    support.timestamp(request.approvedAt()),
                    request.approvalReason(),
                    request.executedBy(),
                    support.timestamp(request.executedAt()),
                    request.rejectedBy(),
                    support.timestamp(request.rejectedAt()),
                    request.rejectionReason()
            );
            return request;
        });
    }

    @Override
    public OperationOutboxRequeueRequestRecord approveManualReviewRequeueRequest(
            String requestId,
            Instant approvedAt,
            String approvedBy,
            String approvalReason
    ) {
        return transactionTemplate.execute(status -> {
            OperationOutboxRequeueRequestRecord request = requeueRequestForUpdate(requestId);
            if (request.status() != OperationOutboxRequeueRequestStatus.REQUESTED) {
                throw new InvalidWalletOperationException("requeue request must be REQUESTED: " + requestId);
            }
            if (request.requestedBy().equals(approvedBy)) {
                throw new InvalidWalletOperationException("approver must be different from requester");
            }
            support.requireSingleRowUpdate(
                    jdbcTemplate.update(
                            """
                                    update operation_outbox_requeue_requests
                                    set status = ?, approved_by = ?, approved_at = ?, approval_reason = ?
                                    where request_id = ?
                                      and status = ?
                                    """,
                            OperationOutboxRequeueRequestStatus.APPROVED.name(),
                            approvedBy,
                            support.timestamp(approvedAt),
                            approvalReason,
                            requestId,
                            OperationOutboxRequeueRequestStatus.REQUESTED.name()
                    ),
                    "requeue request must still be REQUESTED: " + requestId
            );
            return requeueRequest(requestId);
        });
    }

    @Override
    public OperationOutboxRequeueRequestRecord rejectManualReviewRequeueRequest(
            String requestId,
            Instant rejectedAt,
            String rejectedBy,
            String rejectionReason
    ) {
        return transactionTemplate.execute(status -> {
            OperationOutboxRequeueRequestRecord request = requeueRequestForUpdate(requestId);
            if (request.status() != OperationOutboxRequeueRequestStatus.REQUESTED) {
                throw new InvalidWalletOperationException("requeue request must be REQUESTED: " + requestId);
            }
            if (request.requestedBy().equals(rejectedBy)) {
                throw new InvalidWalletOperationException("rejector must be different from requester");
            }
            support.requireSingleRowUpdate(
                    jdbcTemplate.update(
                            """
                                    update operation_outbox_requeue_requests
                                    set status = ?, rejected_by = ?, rejected_at = ?, rejection_reason = ?
                                    where request_id = ?
                                      and status = ?
                                    """,
                            OperationOutboxRequeueRequestStatus.REJECTED.name(),
                            rejectedBy,
                            support.timestamp(rejectedAt),
                            rejectionReason,
                            requestId,
                            OperationOutboxRequeueRequestStatus.REQUESTED.name()
                    ),
                    "requeue request must still be REQUESTED: " + requestId
            );
            return requeueRequest(requestId);
        });
    }

    @Override
    public OperationOutboxRequeueRequestRecord executeManualReviewRequeueRequest(
            String requestId,
            Instant executedAt,
            String executedBy
    ) {
        return transactionTemplate.execute(status -> {
            OperationOutboxRequeueRequestRecord request = requeueRequestForUpdate(requestId);
            if (request.status() != OperationOutboxRequeueRequestStatus.APPROVED) {
                throw new InvalidWalletOperationException("requeue request must be APPROVED: " + requestId);
            }
            OperationOutboxEvent event = manualReviewOutboxEventForUpdate(request.outboxEventId());
            support.requireSingleRowUpdate(
                    jdbcTemplate.update(
                            """
                                    update operation_outbox_events
                                    set status = ?, attempt_count = 0, next_retry_at = null,
                                        claimed_at = null, lease_expires_at = null, published_at = null, last_error = null
                                    where outbox_event_id = ?
                                      and status = ?
                                    """,
                            OperationOutboxStatus.PENDING.name(),
                            request.outboxEventId(),
                            OperationOutboxStatus.MANUAL_REVIEW.name()
                    ),
                    "outbox event must still be MANUAL_REVIEW: " + request.outboxEventId()
            );
            jdbcTemplate.update(
                    """
                            insert into operation_outbox_requeue_audits (
                                audit_id, outbox_event_id, operation_id, requeued_at, operator_name, reason
                            )
                            values (?, ?, ?, ?, ?, ?)
                            """,
                    support.nextId("outbox-requeue-audit", "outbox_requeue_audit_id_seq"),
                    request.outboxEventId(),
                    event.operationId(),
                    support.timestamp(executedAt),
                    executedBy,
                    request.requestReason()
            );
            support.requireSingleRowUpdate(
                    jdbcTemplate.update(
                            """
                                    update operation_outbox_requeue_requests
                                    set status = ?, executed_by = ?, executed_at = ?
                                    where request_id = ?
                                      and status = ?
                                    """,
                            OperationOutboxRequeueRequestStatus.EXECUTED.name(),
                            executedBy,
                            support.timestamp(executedAt),
                            requestId,
                            OperationOutboxRequeueRequestStatus.APPROVED.name()
                    ),
                    "requeue request must still be APPROVED: " + requestId
            );
            return requeueRequest(requestId);
        });
    }

    @Override
    public String nextRelayRunId() {
        return support.nextId("outbox-relay-run", "outbox_relay_run_id_seq");
    }

    @Override
    public void saveOutboxRelayRun(OperationOutboxRelayRun relayRun) {
        jdbcTemplate.update(
                """
                        insert into operation_outbox_relay_runs (
                            relay_run_id, started_at, completed_at, status, batch_size,
                            claimed_count, published_count, failed_count, error_message
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                relayRun.relayRunId(),
                support.timestamp(relayRun.startedAt()),
                support.timestamp(relayRun.completedAt()),
                relayRun.status().name(),
                relayRun.batchSize(),
                relayRun.claimedCount(),
                relayRun.publishedCount(),
                relayRun.failedCount(),
                relayRun.errorMessage()
        );
    }

    @Override
    public List<OperationOutboxRelayRun> findRecentOutboxRelayRuns(int limit) {
        return jdbcTemplate.query(
                """
                        select relay_run_id, started_at, completed_at, status, batch_size,
                               claimed_count, published_count, failed_count, error_message
                        from operation_outbox_relay_runs
                        order by completed_at desc, relay_run_id desc
                        limit ?
                        """,
                support.operationOutboxRelayRunMapper(),
                limit
        );
    }

    @Override
    public int deleteOutboxRelayRunsCompletedBefore(Instant cutoff) {
        return jdbcTemplate.update(
                "delete from operation_outbox_relay_runs where completed_at < ?",
                support.timestamp(cutoff)
        );
    }

    private List<String> claimReadyOutboxEventIds(int limit, Instant now) {
        try {
            return jdbcTemplate.queryForList(
                    """
                            select outbox_event_id
                            from operation_outbox_events
                            where status = ?
                               or (status = ? and (next_retry_at is null or next_retry_at <= ?))
                               or (status = ? and lease_expires_at <= ?)
                            order by occurred_at, outbox_event_id
                            limit ?
                            for update skip locked
                            """,
                    String.class,
                    OperationOutboxStatus.PENDING.name(),
                    OperationOutboxStatus.FAILED.name(),
                    support.timestamp(now),
                    OperationOutboxStatus.PROCESSING.name(),
                    support.timestamp(now),
                    limit
            );
        } catch (BadSqlGrammarException exception) {
            return jdbcTemplate.queryForList(
                    """
                            select outbox_event_id
                            from operation_outbox_events
                            where status = ?
                               or (status = ? and (next_retry_at is null or next_retry_at <= ?))
                               or (status = ? and lease_expires_at <= ?)
                            order by occurred_at, outbox_event_id
                            limit ?
                            for update
                            """,
                    String.class,
                    OperationOutboxStatus.PENDING.name(),
                    OperationOutboxStatus.FAILED.name(),
                    support.timestamp(now),
                    OperationOutboxStatus.PROCESSING.name(),
                    support.timestamp(now),
                    limit
            );
        }
    }

    private OperationOutboxEvent manualReviewOutboxEvent(String outboxEventId) {
        return support.queryOptional(
                """
                        select outbox_event_id, operation_id, event_type, aggregate_type,
                               aggregate_id, payload, status, occurred_at,
                               attempt_count, next_retry_at, claimed_at, lease_expires_at,
                               published_at, last_error
                        from operation_outbox_events
                        where outbox_event_id = ?
                          and status = ?
                        """,
                support.operationOutboxEventMapper(),
                outboxEventId,
                OperationOutboxStatus.MANUAL_REVIEW.name()
        )
                .orElseThrow(() -> new InvalidWalletOperationException("manual review outbox event not found: " + outboxEventId));
    }

    private OperationOutboxEvent manualReviewOutboxEventForUpdate(String outboxEventId) {
        return support.queryOptional(
                """
                        select outbox_event_id, operation_id, event_type, aggregate_type,
                               aggregate_id, payload, status, occurred_at,
                               attempt_count, next_retry_at, claimed_at, lease_expires_at,
                               published_at, last_error
                        from operation_outbox_events
                        where outbox_event_id = ?
                          and status = ?
                        for update
                        """,
                support.operationOutboxEventMapper(),
                outboxEventId,
                OperationOutboxStatus.MANUAL_REVIEW.name()
        )
                .orElseThrow(() -> new InvalidWalletOperationException("manual review outbox event not found: " + outboxEventId));
    }

    private OperationOutboxRequeueRequestRecord requeueRequest(String requestId) {
        return support.queryOptional(
                """
                        select request_id, outbox_event_id, operation_id, status, requested_by,
                               request_reason, requested_at, approved_by, approved_at, approval_reason,
                               executed_by, executed_at, rejected_by, rejected_at, rejection_reason
                        from operation_outbox_requeue_requests
                        where request_id = ?
                        """,
                support.outboxRequeueRequestMapper(),
                requestId
        )
                .orElseThrow(() -> new InvalidWalletOperationException("requeue request not found: " + requestId));
    }

    private OperationOutboxRequeueRequestRecord requeueRequestForUpdate(String requestId) {
        return support.queryOptional(
                """
                        select request_id, outbox_event_id, operation_id, status, requested_by,
                               request_reason, requested_at, approved_by, approved_at, approval_reason,
                               executed_by, executed_at, rejected_by, rejected_at, rejection_reason
                        from operation_outbox_requeue_requests
                        where request_id = ?
                        for update
                        """,
                support.outboxRequeueRequestMapper(),
                requestId
        )
                .orElseThrow(() -> new InvalidWalletOperationException("requeue request not found: " + requestId));
    }

    static String truncateLastError(String lastError) {
        if (lastError == null || lastError.length() <= MAX_LAST_ERROR_LENGTH) {
            return lastError;
        }
        return lastError.substring(0, MAX_LAST_ERROR_LENGTH);
    }
}
