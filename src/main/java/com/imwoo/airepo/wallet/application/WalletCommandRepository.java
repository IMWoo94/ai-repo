package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.Money;
import java.time.Instant;
import java.util.Optional;

public interface WalletCommandRepository extends WalletQueryRepository {

    Optional<WalletOperationRecord> findOperation(String idempotencyKey);

    WalletOperationOutcome applyCharge(
            String idempotencyKey,
            String fingerprint,
            String walletId,
            Money money,
            String description,
            Instant occurredAt
    );

    WalletOperationOutcome applyTransfer(
            String idempotencyKey,
            String fingerprint,
            String sourceWalletId,
            String targetWalletId,
            Money money,
            String description,
            Instant occurredAt
    );
}
