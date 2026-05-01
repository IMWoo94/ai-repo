package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record TransactionHistoryItem(
        String transactionId,
        String walletId,
        Instant occurredAt,
        TransactionType type,
        TransactionStatus status,
        TransactionDirection direction,
        Money money,
        String description
) {

    public TransactionHistoryItem {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(money, "money must not be null");
        Objects.requireNonNull(description, "description must not be null");
        if (transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId must not be blank");
        }
        if (walletId.isBlank()) {
            throw new IllegalArgumentException("walletId must not be blank");
        }
    }
}
