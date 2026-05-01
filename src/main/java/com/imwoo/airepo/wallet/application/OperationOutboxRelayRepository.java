package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import java.time.Instant;
import java.util.List;

public interface OperationOutboxRelayRepository {

    List<OperationOutboxEvent> findPendingOutboxEvents(int limit);

    void markOutboxEventPublished(String outboxEventId, Instant publishedAt);

    void markOutboxEventFailed(String outboxEventId, String lastError);
}
