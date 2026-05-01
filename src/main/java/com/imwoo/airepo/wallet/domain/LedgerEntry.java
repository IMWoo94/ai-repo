package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record LedgerEntry(
        String ledgerEntryId,
        String operationId,
        String walletId,
        Instant occurredAt,
        TransactionType type,
        TransactionDirection direction,
        Money money,
        Money balanceAfter,
        String description
) {

    public LedgerEntry {
        Objects.requireNonNull(ledgerEntryId, "ledgerEntryId must not be null");
        Objects.requireNonNull(operationId, "operationId must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(money, "money must not be null");
        Objects.requireNonNull(balanceAfter, "balanceAfter must not be null");
        Objects.requireNonNull(description, "description must not be null");
        if (ledgerEntryId.isBlank()) {
            throw new IllegalArgumentException("ledgerEntryId must not be blank");
        }
        if (operationId.isBlank()) {
            throw new IllegalArgumentException("operationId must not be blank");
        }
        if (walletId.isBlank()) {
            throw new IllegalArgumentException("walletId must not be blank");
        }
    }
}
