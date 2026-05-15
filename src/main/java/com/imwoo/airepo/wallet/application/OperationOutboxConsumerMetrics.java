package com.imwoo.airepo.wallet.application;

import java.time.Instant;

public record OperationOutboxConsumerMetrics(
        long processedEventCount,
        long duplicateEventCount,
        long receiptCount,
        Instant lastProcessedAt,
        Instant lastReceivedAt
) {
}
