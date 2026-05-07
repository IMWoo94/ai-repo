package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record OperationOutboxRequeueRequestRecord(
        String requestId,
        String outboxEventId,
        String operationId,
        OperationOutboxRequeueRequestStatus status,
        String requestedBy,
        String requestReason,
        Instant requestedAt,
        String approvedBy,
        Instant approvedAt,
        String approvalReason,
        String executedBy,
        Instant executedAt
) {

    public OperationOutboxRequeueRequestRecord {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(outboxEventId, "outboxEventId must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(requestedBy, "requestedBy must not be null");
        Objects.requireNonNull(requestReason, "requestReason must not be null");
        Objects.requireNonNull(requestedAt, "requestedAt must not be null");
        if (requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        if (outboxEventId.isBlank()) {
            throw new IllegalArgumentException("outboxEventId must not be blank");
        }
        if (operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (requestedBy.isBlank()) {
            throw new IllegalArgumentException("requestedBy must not be blank");
        }
        if (requestReason.isBlank()) {
            throw new IllegalArgumentException("requestReason must not be blank");
        }
    }
}
