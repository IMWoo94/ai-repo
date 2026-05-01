package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record WalletBalance(String walletId, Money money, Instant asOf) {

    public WalletBalance {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(money, "money must not be null");
        Objects.requireNonNull(asOf, "asOf must not be null");
        if (walletId.isBlank()) {
            throw new IllegalArgumentException("walletId must not be blank");
        }
    }
}
