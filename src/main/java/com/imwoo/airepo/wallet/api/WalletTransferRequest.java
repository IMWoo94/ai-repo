package com.imwoo.airepo.wallet.api;

import java.math.BigDecimal;

public record WalletTransferRequest(
        String targetWalletId,
        BigDecimal amount,
        String currency,
        String idempotencyKey,
        String description
) {
}
