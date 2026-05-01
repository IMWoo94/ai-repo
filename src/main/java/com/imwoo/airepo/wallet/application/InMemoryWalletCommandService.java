package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class InMemoryWalletCommandService implements WalletCommandService {

    private static final String SUPPORTED_CURRENCY = "KRW";

    private final Clock clock;
    private final WalletCommandRepository walletCommandRepository;

    public InMemoryWalletCommandService(Clock clock, WalletCommandRepository walletCommandRepository) {
        this.clock = clock;
        this.walletCommandRepository = walletCommandRepository;
    }

    @Override
    public synchronized WalletCommandResult charge(String walletId, WalletChargeCommand command) {
        validateWalletId(walletId);
        validateMoney(command.money());
        validateIdempotencyKey(command.idempotencyKey());

        WalletAccount walletAccount = findOperableWallet(walletId);
        String fingerprint = chargeFingerprint(walletAccount.walletId(), command);
        return resolveIdempotency(command.idempotencyKey(), fingerprint)
                .orElseGet(() -> new WalletCommandResult(
                        walletCommandRepository.applyCharge(
                                command.idempotencyKey(),
                                fingerprint,
                                walletAccount.walletId(),
                                command.money(),
                                command.description(),
                                Instant.now(clock)
                        ).result(),
                        true
                ));
    }

    @Override
    public synchronized WalletCommandResult transfer(String sourceWalletId, WalletTransferCommand command) {
        validateWalletId(sourceWalletId);
        validateWalletId(command.targetWalletId());
        validateMoney(command.money());
        validateIdempotencyKey(command.idempotencyKey());
        if (sourceWalletId.equals(command.targetWalletId())) {
            throw new InvalidWalletOperationException("sourceWalletId and targetWalletId must be different");
        }

        WalletAccount sourceWallet = findOperableWallet(sourceWalletId);
        WalletAccount targetWallet = findOperableWallet(command.targetWalletId());
        WalletBalance sourceBalance = walletCommandRepository.findBalance(sourceWallet.walletId())
                .orElseThrow(() -> new WalletNotFoundException(sourceWallet.walletId()));
        if (sourceBalance.money().lessThan(command.money())) {
            throw new InsufficientBalanceException(sourceWallet.walletId());
        }

        String fingerprint = transferFingerprint(sourceWallet.walletId(), targetWallet.walletId(), command);
        return resolveIdempotency(command.idempotencyKey(), fingerprint)
                .orElseGet(() -> new WalletCommandResult(
                        walletCommandRepository.applyTransfer(
                                command.idempotencyKey(),
                                fingerprint,
                                sourceWallet.walletId(),
                                targetWallet.walletId(),
                                command.money(),
                                command.description(),
                                Instant.now(clock)
                        ).result(),
                        true
                ));
    }

    private Optional<WalletCommandResult> resolveIdempotency(String idempotencyKey, String fingerprint) {
        return walletCommandRepository.findOperation(idempotencyKey)
                .map(record -> {
                    if (!record.fingerprint().equals(fingerprint)) {
                        throw new IdempotencyKeyConflictException(idempotencyKey);
                    }
                    return new WalletCommandResult(record.result(), false);
                });
    }

    private WalletAccount findOperableWallet(String walletId) {
        WalletAccount walletAccount = walletCommandRepository.findWalletAccount(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        if (!walletAccount.queryable()) {
            throw new WalletAccountNotQueryableException(walletId);
        }
        Member owner = walletCommandRepository.findMember(walletAccount.memberId())
                .orElseThrow(() -> new WalletAccountNotQueryableException(walletId));
        if (!owner.active()) {
            throw new WalletAccountNotQueryableException(walletId);
        }
        return walletAccount;
    }

    private void validateWalletId(String walletId) {
        if (walletId == null || walletId.isBlank()) {
            throw new InvalidWalletIdException("walletId must not be blank");
        }
    }

    private void validateMoney(Money money) {
        if (!money.positive()) {
            throw new InvalidWalletOperationException("amount must be positive");
        }
        if (!SUPPORTED_CURRENCY.equals(money.currency())) {
            throw new InvalidWalletOperationException("currency must be KRW");
        }
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey.isBlank()) {
            throw new InvalidWalletOperationException("idempotencyKey must not be blank");
        }
    }

    private String chargeFingerprint(String walletId, WalletChargeCommand command) {
        return String.join(
                "|",
                "CHARGE",
                walletId,
                amountFingerprint(command.money()),
                command.money().currency(),
                command.description()
        );
    }

    private String transferFingerprint(String sourceWalletId, String targetWalletId, WalletTransferCommand command) {
        return String.join(
                "|",
                "TRANSFER",
                sourceWalletId,
                targetWalletId,
                amountFingerprint(command.money()),
                command.money().currency(),
                command.description()
        );
    }

    private String amountFingerprint(Money money) {
        return money.amount().stripTrailingZeros().toPlainString();
    }
}
