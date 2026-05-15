package com.imwoo.airepo.wallet.application;

import java.time.Instant;

public record OperationOutboxConsumerWindowMetrics(
        Instant windowStartedAt,
        Instant windowEndedAt,
        long processedDeliveryCount,
        long duplicateDeliveryCount,
        long totalDeliveryCount,
        double duplicateRate
) {

    public OperationOutboxConsumerWindowMetrics(
            Instant windowStartedAt,
            Instant windowEndedAt,
            long processedDeliveryCount,
            long duplicateDeliveryCount
    ) {
        this(
                windowStartedAt,
                windowEndedAt,
                processedDeliveryCount,
                duplicateDeliveryCount,
                processedDeliveryCount + duplicateDeliveryCount,
                processedDeliveryCount + duplicateDeliveryCount == 0
                        ? 0.0
                        : (double) duplicateDeliveryCount / (processedDeliveryCount + duplicateDeliveryCount)
        );
    }
}
