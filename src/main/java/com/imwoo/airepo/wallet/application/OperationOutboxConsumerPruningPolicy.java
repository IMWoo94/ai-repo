package com.imwoo.airepo.wallet.application;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OperationOutboxConsumerPruningPolicy {

    private final Duration processedEventRetention;
    private final Duration receiptRetention;
    private final Duration deliveryMetricRetention;

    public OperationOutboxConsumerPruningPolicy(
            @Value("${ai-repo.outbox-consumer-pruning.processed-event-retention-days:30}")
            int processedEventRetentionDays,
            @Value("${ai-repo.outbox-consumer-pruning.receipt-retention-days:30}")
            int receiptRetentionDays,
            @Value("${ai-repo.outbox-consumer-pruning.delivery-metric-retention-days:30}")
            int deliveryMetricRetentionDays
    ) {
        this.processedEventRetention = retention("processed event retention", processedEventRetentionDays);
        this.receiptRetention = retention("consumer receipt retention", receiptRetentionDays);
        this.deliveryMetricRetention = retention("consumer delivery metric retention", deliveryMetricRetentionDays);
    }

    public Duration processedEventRetention() {
        return processedEventRetention;
    }

    public Duration receiptRetention() {
        return receiptRetention;
    }

    public Duration deliveryMetricRetention() {
        return deliveryMetricRetention;
    }

    private Duration retention(String name, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException(name + " days must be positive");
        }
        return Duration.ofDays(days);
    }
}
