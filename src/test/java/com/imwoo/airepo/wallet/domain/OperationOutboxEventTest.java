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
                Instant.parse("2026-05-01T00:00:00Z"),
                0,
                null,
                null
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
                Instant.parse("2026-05-01T00:00:00Z"),
                0,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload must not be blank");
    }

    @Test
    void rejectsNegativeAttemptCount() {
        assertThatThrownBy(() -> new OperationOutboxEvent(
                "outbox-001",
                "op-001",
                "CHARGE_COMPLETED",
                "WALLET_OPERATION",
                "op-001",
                "{}",
                OperationOutboxStatus.PENDING,
                Instant.parse("2026-05-01T00:00:00Z"),
                -1,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("attemptCount must not be negative");
    }

    @Test
    void requiresPublishedAtWhenPublished() {
        assertThatThrownBy(() -> new OperationOutboxEvent(
                "outbox-001",
                "op-001",
                "CHARGE_COMPLETED",
                "WALLET_OPERATION",
                "op-001",
                "{}",
                OperationOutboxStatus.PUBLISHED,
                Instant.parse("2026-05-01T00:00:00Z"),
                0,
                null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("publishedAt must not be null when status is PUBLISHED");
    }

    @Test
    void requiresLastErrorWhenFailed() {
        assertThatThrownBy(() -> new OperationOutboxEvent(
                "outbox-001",
                "op-001",
                "CHARGE_COMPLETED",
                "WALLET_OPERATION",
                "op-001",
                "{}",
                OperationOutboxStatus.FAILED,
                Instant.parse("2026-05-01T00:00:00Z"),
                1,
                null,
                " "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("lastError must not be blank when status is FAILED");
    }
}
