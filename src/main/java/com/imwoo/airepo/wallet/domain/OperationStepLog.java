package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record OperationStepLog(
        String operationStepLogId,
        String operationId,
        OperationStep step,
        TransactionStatus status,
        Instant occurredAt,
        String detail
) {

    public OperationStepLog {
        Objects.requireNonNull(operationStepLogId, "operationStepLogId must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(step, "step must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(detail, "detail must not be null");
        if (operationStepLogId.isBlank()) {
            throw new IllegalArgumentException("operationStepLogId must not be blank");
        }
        if (operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (detail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
    }
}
