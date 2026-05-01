package com.imwoo.airepo.wallet.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1"), "KRW"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("amount must not be negative");
    }

    @Test
    void rejectsBlankCurrency() {
        assertThatThrownBy(() -> new Money(BigDecimal.ZERO, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currency must not be blank");
    }
}
