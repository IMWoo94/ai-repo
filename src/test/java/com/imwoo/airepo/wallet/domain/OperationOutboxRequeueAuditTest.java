package com.imwoo.airepo.wallet.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OperationOutboxRequeueAuditTest {

    @Test
    void rejectsBlankOperator() {
        assertThatThrownBy(() -> new OperationOutboxRequeueAudit(
                "audit-001",
                "outbox-001",
                "op-001",
                Instant.parse("2026-05-01T00:00:00Z"),
                " ",
                "broker recovered"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operator must not be blank");
    }

    @Test
    void rejectsBlankReason() {
        assertThatThrownBy(() -> new OperationOutboxRequeueAudit(
                "audit-001",
                "outbox-001",
                "op-001",
                Instant.parse("2026-05-01T00:00:00Z"),
                "ops-user",
                " "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("reason must not be blank");
    }
}
