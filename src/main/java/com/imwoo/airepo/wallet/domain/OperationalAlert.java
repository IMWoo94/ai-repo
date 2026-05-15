package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record OperationalAlert(
        String alertId,
        String source,
        OperationalAlertSeverity severity,
        Instant occurredAt,
        List<String> reasons
) {

    public OperationalAlert {
        Objects.requireNonNull(alertId, "alertId must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(reasons, "reasons must not be null");
        if (alertId.isBlank()) {
            throw new IllegalArgumentException("alertId must not be blank");
        }
        if (source.isBlank()) {
            throw new IllegalArgumentException("source must not be blank");
        }
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("reasons must not be empty");
        }
        reasons = List.copyOf(reasons);
    }
}
