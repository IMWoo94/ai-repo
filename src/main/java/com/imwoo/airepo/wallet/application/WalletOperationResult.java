package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.TransactionDirection;
import com.imwoo.airepo.wallet.domain.TransactionStatus;
import com.imwoo.airepo.wallet.domain.TransactionType;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.time.Instant;
import java.util.Objects;

public record WalletOperationResult(
        String transactionId,
        String walletId,
        String counterpartyWalletId,
        Instant occurredAt,
        TransactionType type,
        TransactionStatus status,
        TransactionDirection direction,
        Money money,
        WalletBalance balance,
        String description
) {

    public WalletOperationResult {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(money, "money must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
        Objects.requireNonNull(description, "description must not be null");
    }
}
