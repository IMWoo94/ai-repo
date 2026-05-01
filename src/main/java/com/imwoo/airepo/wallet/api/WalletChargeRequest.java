package com.imwoo.airepo.wallet.api;

import java.math.BigDecimal;

public record WalletChargeRequest(
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String description
) {
}
