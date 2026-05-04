package com.imwoo.airepo.wallet.application;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OutboxRelayHealthPolicy {

    private final int sampleSize;
    private final int warningConsecutiveFailures;
    private final int criticalConsecutiveFailures;
    private final double warningFailureRate;
    private final Duration criticalLastSuccessAge;

    public OutboxRelayHealthPolicy(
            @Value("${ai-repo.outbox-relay.health.sample-size:20}") int sampleSize,
            @Value("${ai-repo.outbox-relay.health.warning-consecutive-failures:2}") int warningConsecutiveFailures,
            @Value("${ai-repo.outbox-relay.health.critical-consecutive-failures:3}") int criticalConsecutiveFailures,
            @Value("${ai-repo.outbox-relay.health.warning-failure-rate-percent:50}") double warningFailureRatePercent,
            @Value("${ai-repo.outbox-relay.health.critical-last-success-age-minutes:15}") int criticalLastSuccessAgeMinutes
    ) {
        if (sampleSize <= 0) {
            throw new IllegalArgumentException("outbox relay health sample-size must be positive");
        }
        if (warningConsecutiveFailures <= 0) {
            throw new IllegalArgumentException("outbox relay health warning-consecutive-failures must be positive");
        }
        if (criticalConsecutiveFailures < warningConsecutiveFailures) {
            throw new IllegalArgumentException(
                    "outbox relay health critical-consecutive-failures must be greater than or equal to warning threshold"
            );
        }
        if (warningFailureRatePercent <= 0 || warningFailureRatePercent > 100) {
            throw new IllegalArgumentException("outbox relay health warning-failure-rate-percent must be 1..100");
        }
        if (criticalLastSuccessAgeMinutes <= 0) {
            throw new IllegalArgumentException("outbox relay health critical-last-success-age-minutes must be positive");
        }
        this.sampleSize = sampleSize;
        this.warningConsecutiveFailures = warningConsecutiveFailures;
        this.criticalConsecutiveFailures = criticalConsecutiveFailures;
        this.warningFailureRate = warningFailureRatePercent / 100.0;
        this.criticalLastSuccessAge = Duration.ofMinutes(criticalLastSuccessAgeMinutes);
    }

    public int sampleSize() {
        return sampleSize;
    }

    public int warningConsecutiveFailures() {
        return warningConsecutiveFailures;
    }

    public int criticalConsecutiveFailures() {
        return criticalConsecutiveFailures;
    }

    public double warningFailureRate() {
        return warningFailureRate;
    }

    public Duration criticalLastSuccessAge() {
        return criticalLastSuccessAge;
    }
}
