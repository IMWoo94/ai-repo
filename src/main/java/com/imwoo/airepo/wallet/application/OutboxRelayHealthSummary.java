package com.imwoo.airepo.wallet.application;

import java.time.Instant;
import java.util.List;

public record OutboxRelayHealthSummary(
        Instant evaluatedAt,
        OutboxRelayHealthStatus status,
        int sampleSize,
        int totalRunCount,
        int successCount,
        int failedCount,
        double failureRate,
        int consecutiveFailureCount,
        Instant lastCompletedAt,
        Instant lastSuccessAt,
        Instant lastFailureAt,
        List<String> alertReasons
) {

    public OutboxRelayHealthSummary {
        alertReasons = List.copyOf(alertReasons);
    }
}
