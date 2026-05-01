package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import java.time.Instant;
import java.util.List;

public interface OperationOutboxRelayRepository {

    List<OperationOutboxEvent> findPendingOutboxEvents(int limit);

    List<OperationOutboxEvent> claimReadyOutboxEvents(int limit, Instant now, Instant leaseExpiresAt);

    void markOutboxEventPublished(String outboxEventId, Instant publishedAt);

    void markOutboxEventFailed(String outboxEventId, String lastError, Instant nextRetryAt, int maxAttempts);
}
