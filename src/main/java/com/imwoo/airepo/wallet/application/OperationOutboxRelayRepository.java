package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueAudit;
import java.time.Instant;
import java.util.List;

public interface OperationOutboxRelayRepository {

    List<OperationOutboxEvent> findPendingOutboxEvents(int limit);

    List<OperationOutboxEvent> findManualReviewOutboxEvents(int limit);

    List<OperationOutboxRequeueAudit> findOutboxRequeueAudits(String outboxEventId);

    List<OperationOutboxEvent> claimReadyOutboxEvents(int limit, Instant now, Instant leaseExpiresAt);

    void markOutboxEventPublished(String outboxEventId, Instant publishedAt);

    void markOutboxEventFailed(String outboxEventId, String lastError, Instant nextRetryAt, int maxAttempts);

    void requeueManualReviewOutboxEvent(String outboxEventId, Instant requeuedAt, String operator, String reason);
}
