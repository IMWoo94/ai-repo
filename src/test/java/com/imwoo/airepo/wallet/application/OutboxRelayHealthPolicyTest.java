package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxRelayHealthPolicyTest {

    @Test
    void convertsConfiguredPercentAndMinutesToRuntimeThresholds() {
        OutboxRelayHealthPolicy policy = new OutboxRelayHealthPolicy(7, 2, 4, 25, 30);

        assertThat(policy.sampleSize()).isEqualTo(7);
        assertThat(policy.warningConsecutiveFailures()).isEqualTo(2);
        assertThat(policy.criticalConsecutiveFailures()).isEqualTo(4);
        assertThat(policy.warningFailureRate()).isEqualTo(0.25);
        assertThat(policy.criticalLastSuccessAge()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void rejectsInvalidThresholdConfiguration() {
        assertThatThrownBy(() -> new OutboxRelayHealthPolicy(0, 2, 4, 25, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox relay health sample-size must be positive");
        assertThatThrownBy(() -> new OutboxRelayHealthPolicy(7, 0, 4, 25, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox relay health warning-consecutive-failures must be positive");
        assertThatThrownBy(() -> new OutboxRelayHealthPolicy(7, 3, 2, 25, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox relay health critical-consecutive-failures must be greater than or equal to warning threshold");
        assertThatThrownBy(() -> new OutboxRelayHealthPolicy(7, 2, 4, 0, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox relay health warning-failure-rate-percent must be 1..100");
        assertThatThrownBy(() -> new OutboxRelayHealthPolicy(7, 2, 4, 101, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox relay health warning-failure-rate-percent must be 1..100");
        assertThatThrownBy(() -> new OutboxRelayHealthPolicy(7, 2, 4, 25, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox relay health critical-last-success-age-minutes must be positive");
    }
}
