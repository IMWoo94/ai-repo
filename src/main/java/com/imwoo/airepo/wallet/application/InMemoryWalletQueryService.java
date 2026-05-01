package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.TransactionHistoryItem;
import com.imwoo.airepo.wallet.domain.WalletAccount;
import com.imwoo.airepo.wallet.domain.WalletBalance;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InMemoryWalletQueryService implements WalletQueryService {

    private final Clock clock;
    private final WalletQueryRepository walletQueryRepository;

    public InMemoryWalletQueryService(Clock clock, WalletQueryRepository walletQueryRepository) {
        this.clock = clock;
        this.walletQueryRepository = walletQueryRepository;
    }

    @Override
    public WalletBalance getBalance(String walletId) {
        validateWalletId(walletId);
        WalletAccount walletAccount = findQueryableWallet(walletId);
        WalletBalance balance = walletQueryRepository.findBalance(walletAccount.walletId())
                .orElseThrow(() -> walletNotFound(walletId));
        return new WalletBalance(balance.walletId(), balance.money(), Instant.now(clock));
    }

    @Override
    public List<TransactionHistoryItem> getTransactions(String walletId) {
        validateWalletId(walletId);
        WalletAccount walletAccount = findQueryableWallet(walletId);
        return walletQueryRepository.findTransactions(walletAccount.walletId()).stream()
                .sorted(Comparator.comparing(TransactionHistoryItem::occurredAt).reversed())
                .toList();
    }

    private WalletAccount findQueryableWallet(String walletId) {
        WalletAccount walletAccount = walletQueryRepository.findWalletAccount(walletId)
                .orElseThrow(() -> walletNotFound(walletId));
        if (!walletAccount.queryable()) {
            throw new WalletAccountNotQueryableException(walletId);
        }
        Member owner = walletQueryRepository.findMember(walletAccount.memberId())
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

    private WalletNotFoundException walletNotFound(String walletId) {
        return new WalletNotFoundException(walletId);
    }
}
