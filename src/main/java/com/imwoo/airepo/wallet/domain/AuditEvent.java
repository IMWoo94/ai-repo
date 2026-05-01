package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record AuditEvent(
        String auditEventId,
        String operationId,
        AuditEventType type,
        Instant occurredAt,
        String detail
) {

    public AuditEvent {
        Objects.requireNonNull(auditEventId, "auditEventId must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(detail, "detail must not be null");
        if (auditEventId.isBlank()) {
            throw new IllegalArgumentException("auditEventId must not be blank");
        }
        if (operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
    }
}
