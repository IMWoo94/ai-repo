package com.imwoo.airepo.wallet.application;

import java.util.Objects;

public record WalletCommandResult(WalletOperationResult operation, boolean created) {

    public WalletCommandResult {
        Objects.requireNonNull(operation, "operation must not be null");
    }
}
