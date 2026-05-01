package com.imwoo.airepo.wallet.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditEventTest {

    @Test
    void rejectsBlankAuditEventId() {
        assertThatThrownBy(() -> new AuditEvent(
                " ",
                "op-001",
                AuditEventType.CHARGE_COMPLETED,
                Instant.parse("2026-05-01T00:00:00Z"),
                "Charge completed"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("auditEventId must not be blank");
    }
}
