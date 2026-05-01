package com.imwoo.airepo.wallet.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class OperationStepLogTest {

    @Test
    void rejectsBlankOperationStepLogId() {
        assertThatThrownBy(() -> new OperationStepLog(
                " ",
                "op-001",
                OperationStep.BALANCE_LOCKED,
                TransactionStatus.COMPLETED,
                Instant.parse("2026-05-01T00:00:00Z"),
                "Balance locked"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operationStepLogId must not be blank");
    }

    @Test
    void rejectsBlankDetail() {
        assertThatThrownBy(() -> new OperationStepLog(
                "step-001",
                "op-001",
                OperationStep.BALANCE_LOCKED,
                TransactionStatus.COMPLETED,
                Instant.parse("2026-05-01T00:00:00Z"),
                " "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("detail must not be blank");
    }
}
