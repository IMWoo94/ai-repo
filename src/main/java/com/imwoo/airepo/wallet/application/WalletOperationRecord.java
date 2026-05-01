package com.imwoo.airepo.wallet.application;

import java.util.Objects;

public record WalletOperationRecord(String idempotencyKey, String fingerprint, WalletOperationResult result) {

    public WalletOperationRecord {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(fingerprint, "fingerprint must not be null");
        Objects.requireNonNull(result, "result must not be null");
    }
}
