package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.Money;
import java.util.Objects;

public record WalletChargeCommand(Money money, String idempotencyKey, String description) {

    public WalletChargeCommand {
        Objects.requireNonNull(money, "money must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }
}
