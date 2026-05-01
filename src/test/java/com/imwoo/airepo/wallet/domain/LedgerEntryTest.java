package com.imwoo.airepo.wallet.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LedgerEntryTest {

    @Test
    void rejectsBlankOperationId() {
        assertThatThrownBy(() -> new LedgerEntry(
                "ledger-001",
                " ",
                "wallet-001",
                Instant.parse("2026-05-01T00:00:00Z"),
                TransactionType.CHARGE,
                TransactionDirection.CREDIT,
                new Money(new BigDecimal("1000"), "KRW"),
                new Money(new BigDecimal("126000"), "KRW"),
                "테스트 충전"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("operationId must not be blank");
    }
}
