package com.imwoo.airepo.wallet.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OperationOutboxConsumerHealthPolicy {

    private final long minDuplicateEventCount;
    private final double warningDuplicateRate;
    private final double criticalDuplicateRate;

    public OperationOutboxConsumerHealthPolicy(
            @Value("${ai-repo.outbox-consumer.health.min-duplicate-event-count:5}") long minDuplicateEventCount,
            @Value("${ai-repo.outbox-consumer.health.warning-duplicate-rate-percent:20}")
            double warningDuplicateRatePercent,
            @Value("${ai-repo.outbox-consumer.health.critical-duplicate-rate-percent:50}")
            double criticalDuplicateRatePercent
    ) {
        if (minDuplicateEventCount <= 0) {
            throw new IllegalArgumentException("outbox consumer health min-duplicate-event-count must be positive");
        }
        if (warningDuplicateRatePercent <= 0 || warningDuplicateRatePercent > 100) {
            throw new IllegalArgumentException("outbox consumer health warning-duplicate-rate-percent must be 1..100");
        }
        if (criticalDuplicateRatePercent < warningDuplicateRatePercent || criticalDuplicateRatePercent > 100) {
            throw new IllegalArgumentException(
                    "outbox consumer health critical-duplicate-rate-percent must be warning..100"
            );
        }
        this.minDuplicateEventCount = minDuplicateEventCount;
        this.warningDuplicateRate = warningDuplicateRatePercent / 100.0;
        this.criticalDuplicateRate = criticalDuplicateRatePercent / 100.0;
    }

    public long minDuplicateEventCount() {
        return minDuplicateEventCount;
    }

    public double warningDuplicateRate() {
        return warningDuplicateRate;
    }

    public double criticalDuplicateRate() {
        return criticalDuplicateRate;
    }
}
