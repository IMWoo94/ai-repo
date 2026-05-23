package com.imwoo.airepo.wallet.application;

import java.time.Instant;
import java.util.List;

public record OperationOutboxConsumerHealthSummary(
        Instant evaluatedAt,
        OperationOutboxConsumerHealthStatus status,
        long processedEventCount,
        long duplicateEventCount,
        long receiptCount,
        double duplicateRate,
        Instant windowStartedAt,
        Instant windowEndedAt,
        long windowProcessedDeliveryCount,
        long windowDuplicateDeliveryCount,
        double windowDuplicateRate,
        long windowMinutes,
        long minDuplicateEventCount,
        double warningDuplicateRate,
        double criticalDuplicateRate,
        List<String> alertReasons
) {

    public OperationOutboxConsumerHealthSummary {
        alertReasons = List.copyOf(alertReasons);
    }
}
