package com.imwoo.airepo.wallet.application;

import java.util.Objects;

public record WalletOperationOutcome(WalletOperationRecord record, boolean created) {

    public WalletOperationOutcome {
        Objects.requireNonNull(record, "record must not be null");
    }

    public static WalletOperationOutcome created(WalletOperationRecord record) {
        return new WalletOperationOutcome(record, true);
    }

    public static WalletOperationOutcome recovered(WalletOperationRecord record) {
        return new WalletOperationOutcome(record, false);
    }
}
