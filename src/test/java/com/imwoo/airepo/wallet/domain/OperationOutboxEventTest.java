package com.imwoo.airepo.wallet.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OperationOutboxEventTest {

    @Test
    void rejectsBlankOutboxEventId() {
        assertThatThrownBy(() -> new OperationOutboxEvent(
                " ",
                "op-001",
                "CHARGE_COMPLETED",
                "WALLET_OPERATION",
                "op-001",
                "{}",
                OperationOutboxStatus.PENDING,
                Instant.parse("2026-05-01T00:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outboxEventId must not be blank");
    }

    @Test
    void rejectsBlankPayload() {
        assertThatThrownBy(() -> new OperationOutboxEvent(
                "outbox-001",
                "op-001",
                "CHARGE_COMPLETED",
                "WALLET_OPERATION",
                "op-001",
                " ",
                OperationOutboxStatus.PENDING,
                Instant.parse("2026-05-01T00:00:00Z")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload must not be blank");
    }
}
