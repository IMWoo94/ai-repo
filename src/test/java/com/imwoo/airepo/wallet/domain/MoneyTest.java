package com.imwoo.airepo.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void addsSameCurrencyMoney() {
        Money money = new Money(new BigDecimal("1000"), "KRW");

        assertThat(money.add(new Money(new BigDecimal("500"), "KRW")))
                .isEqualTo(new Money(new BigDecimal("1500"), "KRW"));
    }

    @Test
    void subtractsSameCurrencyMoney() {
        Money money = new Money(new BigDecimal("1000"), "KRW");

        assertThat(money.subtract(new Money(new BigDecimal("500"), "KRW")))
                .isEqualTo(new Money(new BigDecimal("500"), "KRW"));
    }

    @Test
    void rejectsDifferentCurrencyArithmetic() {
        Money money = new Money(new BigDecimal("1000"), "KRW");

        assertThatThrownBy(() -> money.add(new Money(new BigDecimal("1"), "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("currency must be same");
    }
}
