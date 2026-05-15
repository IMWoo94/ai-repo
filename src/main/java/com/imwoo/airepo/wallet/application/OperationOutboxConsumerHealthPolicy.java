package com.imwoo.airepo.wallet.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OperationOutboxConsumerHealthPolicy {

    private final long minDuplicateEventCount;
    private final double warningDuplicateRate;
    private final double criticalDuplicateRate;
    private final long windowMinutes;

    public OperationOutboxConsumerHealthPolicy(
            @Value("${ai-repo.outbox-consumer.health.min-duplicate-event-count:5}") long minDuplicateEventCount,
            @Value("${ai-repo.outbox-consumer.health.warning-duplicate-rate-percent:20}")
            double warningDuplicateRatePercent,
            @Value("${ai-repo.outbox-consumer.health.critical-duplicate-rate-percent:50}")
            double criticalDuplicateRatePercent,
            @Value("${ai-repo.outbox-consumer.health.window-minutes:5}") long windowMinutes
    ) {
        if (minDuplicateEventCount <= 0) {
            throw new IllegalArgumentException("outbox consumer health min-duplicate-event-count must be positive");
        }
        if (windowMinutes <= 0 || windowMinutes > 1440) {
            throw new IllegalArgumentException("outbox consumer health window-minutes must be 1..1440");
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
        this.windowMinutes = windowMinutes;
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

    public long windowMinutes() {
        return windowMinutes;
    }
}
