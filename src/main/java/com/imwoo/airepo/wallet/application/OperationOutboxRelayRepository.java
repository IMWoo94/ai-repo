package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueAudit;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueRequestRecord;
import java.time.Instant;
import java.util.List;

public interface OperationOutboxRelayRepository {

    List<OperationOutboxEvent> findPendingOutboxEvents(int limit);

    List<OperationOutboxEvent> findManualReviewOutboxEvents(int limit);

    List<OperationOutboxRequeueAudit> findOutboxRequeueAudits(String outboxEventId);

    List<OperationOutboxRequeueRequestRecord> findOutboxRequeueRequests(String outboxEventId);

    List<OperationOutboxEvent> claimReadyOutboxEvents(int limit, Instant now, Instant leaseExpiresAt);

    void markOutboxEventPublished(String outboxEventId, Instant publishedAt);

    void markClaimedOutboxEventPublished(String outboxEventId, Instant claimedAt, Instant leaseExpiresAt, Instant publishedAt);

    void markOutboxEventFailed(String outboxEventId, String lastError, Instant nextRetryAt, int maxAttempts);

    void markClaimedOutboxEventFailed(
            String outboxEventId,
            Instant claimedAt,
            Instant leaseExpiresAt,
            String lastError,
            Instant nextRetryAt,
            int maxAttempts
    );

    void requeueManualReviewOutboxEvent(String outboxEventId, Instant requeuedAt, String operator, String reason);

    OperationOutboxRequeueRequestRecord requestManualReviewRequeue(
            String outboxEventId,
            Instant requestedAt,
            String requestedBy,
            String reason
    );

    OperationOutboxRequeueRequestRecord approveManualReviewRequeueRequest(
            String requestId,
            Instant approvedAt,
            String approvedBy,
            String approvalReason
    );

    OperationOutboxRequeueRequestRecord executeManualReviewRequeueRequest(
            String requestId,
            Instant executedAt,
            String executedBy
    );

    OperationOutboxRequeueRequestRecord rejectManualReviewRequeueRequest(
            String requestId,
            Instant rejectedAt,
            String rejectedBy,
            String rejectionReason
    );
}
