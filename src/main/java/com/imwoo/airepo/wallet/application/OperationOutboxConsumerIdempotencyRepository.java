package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerProcessedEvent;
import java.time.Instant;
import java.util.Optional;

public interface OperationOutboxConsumerIdempotencyRepository {

    boolean recordProcessedEvent(
            String idempotencyKey,
            String outboxEventId,
            String eventType,
            Instant processedAt
    );

    Optional<OperationOutboxConsumerProcessedEvent> findProcessedEvent(String idempotencyKey);
}
