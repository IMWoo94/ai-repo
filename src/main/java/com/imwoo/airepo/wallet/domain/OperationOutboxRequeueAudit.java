package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record OperationOutboxRequeueAudit(
        String auditId,
        String outboxEventId,
        String operationId,
        Instant requeuedAt,
        String operator,
        String reason
) {

    public OperationOutboxRequeueAudit {
        Objects.requireNonNull(auditId, "auditId must not be null");
        Objects.requireNonNull(outboxEventId, "outboxEventId must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(requeuedAt, "requeuedAt must not be null");
        Objects.requireNonNull(operator, "operator must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        if (auditId.isBlank()) {
            throw new IllegalArgumentException("auditId must not be blank");
        }
        if (outboxEventId.isBlank()) {
            throw new IllegalArgumentException("outboxEventId must not be blank");
        }
        if (operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (operator.isBlank()) {
            throw new IllegalArgumentException("operator must not be blank");
        }
        if (reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }
}
