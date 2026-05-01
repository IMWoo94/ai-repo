package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record OperationOutboxEvent(
        String outboxEventId,
        String operationId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String payload,
        OperationOutboxStatus status,
        Instant occurredAt,
        int attemptCount,
        Instant publishedAt,
        String lastError
) {

    public OperationOutboxEvent {
        Objects.requireNonNull(outboxEventId, "outboxEventId must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (outboxEventId.isBlank()) {
            throw new IllegalArgumentException("outboxEventId must not be blank");
        }
        if (operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (eventType.isBlank()) {
            throw new IllegalArgumentException("eventType must not be blank");
        }
        if (aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType must not be blank");
        }
        if (aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId must not be blank");
        }
        if (payload.isBlank()) {
            throw new IllegalArgumentException("payload must not be blank");
        }
        if (attemptCount < 0) {
            throw new IllegalArgumentException("attemptCount must not be negative");
        }
        if (status == OperationOutboxStatus.PUBLISHED && publishedAt == null) {
            throw new IllegalArgumentException("publishedAt must not be null when status is PUBLISHED");
        }
        if (status == OperationOutboxStatus.FAILED && (lastError == null || lastError.isBlank())) {
            throw new IllegalArgumentException("lastError must not be blank when status is FAILED");
        }
    }
}
