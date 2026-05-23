package com.imwoo.airepo.wallet.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class OperationOutboxConsumerPruningService {

    private final OperationOutboxConsumerPruningRepository pruningRepository;
    private final Clock clock;

    public OperationOutboxConsumerPruningService(
            OperationOutboxConsumerPruningRepository pruningRepository,
            Clock clock
    ) {
        this.pruningRepository = pruningRepository;
        this.clock = clock;
    }

    public OperationOutboxConsumerPruningResult prune(
            Duration processedEventRetention,
            Duration receiptRetention,
            Duration deliveryMetricRetention
    ) {
        validateRetention("processedEventRetention", processedEventRetention);
        validateRetention("receiptRetention", receiptRetention);
        validateRetention("deliveryMetricRetention", deliveryMetricRetention);
        Instant prunedAt = Instant.now(clock);
        Instant processedEventCutoff = prunedAt.minus(processedEventRetention);
        Instant receiptCutoff = prunedAt.minus(receiptRetention);
        Instant deliveryMetricCutoff = prunedAt.minus(deliveryMetricRetention);
        int deletedReceiptCount = pruningRepository.deleteConsumerReceiptsReceivedBefore(receiptCutoff);
        int deletedProcessedEventCount = pruningRepository.deleteConsumerProcessedEventsProcessedBefore(
                processedEventCutoff
        );
        int deletedDeliveryMetricBucketCount = pruningRepository.deleteConsumerDeliveryMetricsBucketStartedBefore(
                deliveryMetricCutoff
        );
        return new OperationOutboxConsumerPruningResult(
                prunedAt,
                processedEventCutoff,
                receiptCutoff,
                deliveryMetricCutoff,
                deletedProcessedEventCount,
                deletedReceiptCount,
                deletedDeliveryMetricBucketCount
        );
    }

    private void validateRetention(String fieldName, Duration retention) {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new InvalidWalletOperationException(fieldName + " must be positive");
        }
    }
}
