package com.imwoo.airepo.wallet.application;

import java.time.Instant;
import java.util.Objects;

public record OperationOutboxConsumerEnvelope(
        int schemaVersion,
        String idempotencyKey,
        String outboxEventId,
        String operationId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        String payload
) {

    public OperationOutboxConsumerEnvelope {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(outboxEventId, "outboxEventId must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        requireNotBlank("idempotencyKey", idempotencyKey);
        requireNotBlank("outboxEventId", outboxEventId);
        requireNotBlank("operationId", operationId);
        requireNotBlank("eventType", eventType);
        requireNotBlank("aggregateType", aggregateType);
        requireNotBlank("aggregateId", aggregateId);
        requireNotBlank("payload", payload);
    }

    private static void requireNotBlank(String fieldName, String value) {
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
