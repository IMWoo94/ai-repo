package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record WalletAccount(
        String walletId,
        String memberId,
        WalletAccountStatus status,
        Instant createdAt
) {

    public WalletAccount {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (walletId.isBlank()) {
            throw new IllegalArgumentException("walletId must not be blank");
        }
        if (memberId.isBlank()) {
            throw new IllegalArgumentException("memberId must not be blank");
        }
    }

    public boolean queryable() {
        return status == WalletAccountStatus.ACTIVE;
    }
}
