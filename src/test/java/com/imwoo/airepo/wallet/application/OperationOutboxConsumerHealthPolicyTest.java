package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OperationOutboxConsumerHealthPolicyTest {

    @Test
    void convertsConfiguredPercentToRuntimeDuplicateRates() {
        OperationOutboxConsumerHealthPolicy policy = new OperationOutboxConsumerHealthPolicy(3, 15, 65, 10);

        assertThat(policy.minDuplicateEventCount()).isEqualTo(3);
        assertThat(policy.warningDuplicateRate()).isEqualTo(0.15);
        assertThat(policy.criticalDuplicateRate()).isEqualTo(0.65);
        assertThat(policy.windowMinutes()).isEqualTo(10);
    }

    @Test
    void rejectsInvalidDuplicateHealthConfiguration() {
        assertThatThrownBy(() -> new OperationOutboxConsumerHealthPolicy(0, 20, 50, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox consumer health min-duplicate-event-count must be positive");
        assertThatThrownBy(() -> new OperationOutboxConsumerHealthPolicy(3, 20, 50, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox consumer health window-minutes must be 1..1440");
        assertThatThrownBy(() -> new OperationOutboxConsumerHealthPolicy(3, 20, 50, 1441))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox consumer health window-minutes must be 1..1440");
        assertThatThrownBy(() -> new OperationOutboxConsumerHealthPolicy(3, 0, 50, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox consumer health warning-duplicate-rate-percent must be 1..100");
        assertThatThrownBy(() -> new OperationOutboxConsumerHealthPolicy(3, 101, 101, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox consumer health warning-duplicate-rate-percent must be 1..100");
        assertThatThrownBy(() -> new OperationOutboxConsumerHealthPolicy(3, 20, 19, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox consumer health critical-duplicate-rate-percent must be warning..100");
        assertThatThrownBy(() -> new OperationOutboxConsumerHealthPolicy(3, 20, 101, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox consumer health critical-duplicate-rate-percent must be warning..100");
    }
}
