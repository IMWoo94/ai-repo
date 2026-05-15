package com.imwoo.airepo.wallet.application;

import java.time.Instant;

public record OperationOutboxConsumerPruningResult(
        Instant prunedAt,
        Instant processedEventCutoff,
        Instant receiptCutoff,
        int deletedProcessedEventCount,
        int deletedReceiptCount
) {
}
