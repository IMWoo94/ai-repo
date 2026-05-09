package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record OperationOutboxConsumerProcessedEvent(
        String idempotencyKey,
        String outboxEventId,
        String eventType,
        Instant processedAt
) {

    public OperationOutboxConsumerProcessedEvent {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(outboxEventId, "outboxEventId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(processedAt, "processedAt must not be null");
        if (idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
        if (outboxEventId.isBlank()) {
            throw new IllegalArgumentException("outboxEventId must not be blank");
        }
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
    }
}
