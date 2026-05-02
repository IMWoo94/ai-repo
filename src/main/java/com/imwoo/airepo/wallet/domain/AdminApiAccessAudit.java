package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record AdminApiAccessAudit(
        String auditId,
        Instant occurredAt,
        String method,
        String path,
        String operatorId,
        int statusCode,
        AdminApiAccessOutcome outcome
) {

    public AdminApiAccessAudit {
        Objects.requireNonNull(auditId, "auditId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
        if (auditId.isBlank()) {
            throw new IllegalArgumentException("auditId must not be blank");
        }
        if (method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }
        if (path.isBlank()) {
            throw new IllegalArgumentException("path must not be blank");
        }
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("statusCode must be a valid HTTP status code");
        }
        if (operatorId != null && operatorId.isBlank()) {
            throw new IllegalArgumentException("operatorId must not be blank");
        }
    }
}
