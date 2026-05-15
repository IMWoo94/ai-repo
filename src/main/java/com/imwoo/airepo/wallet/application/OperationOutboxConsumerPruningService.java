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
            Duration receiptRetention
    ) {
        validateRetention("processedEventRetention", processedEventRetention);
        validateRetention("receiptRetention", receiptRetention);
        Instant prunedAt = Instant.now(clock);
        Instant processedEventCutoff = prunedAt.minus(processedEventRetention);
        Instant receiptCutoff = prunedAt.minus(receiptRetention);
        int deletedReceiptCount = pruningRepository.deleteConsumerReceiptsReceivedBefore(receiptCutoff);
        int deletedProcessedEventCount = pruningRepository.deleteConsumerProcessedEventsProcessedBefore(
                processedEventCutoff
        );
        return new OperationOutboxConsumerPruningResult(
                prunedAt,
                processedEventCutoff,
                receiptCutoff,
                deletedProcessedEventCount,
                deletedReceiptCount
        );
    }

    private void validateRetention(String fieldName, Duration retention) {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new InvalidWalletOperationException(fieldName + " must be positive");
        }
    }
}
