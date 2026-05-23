package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record OperationOutboxConsumerReceipt(
        String idempotencyKey,
        String outboxEventId,
        String operationId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant receivedAt
) {

    public OperationOutboxConsumerReceipt {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(outboxEventId, "outboxEventId must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        requireNotBlank("idempotencyKey", idempotencyKey);
        requireNotBlank("outboxEventId", outboxEventId);
        requireNotBlank("operationId", operationId);
        requireNotBlank("eventType", eventType);
        requireNotBlank("aggregateType", aggregateType);
        requireNotBlank("aggregateId", aggregateId);
    }

    private static void requireNotBlank(String fieldName, String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
