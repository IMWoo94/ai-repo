package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.WalletAccount;

final class WalletAccessPolicy {

    private WalletAccessPolicy() {
    }

    static WalletAccount findQueryableWallet(WalletQueryRepository walletQueryRepository, String walletId) {
        WalletAccount walletAccount = walletQueryRepository.findWalletAccount(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        requireQueryable(walletQueryRepository, walletAccount);
        return walletAccount;
    }

    static WalletAccount findOwnedQueryableWallet(
            WalletQueryRepository walletQueryRepository,
            String walletId,
            String memberId
    ) {
        WalletAccount walletAccount = walletQueryRepository.findWalletAccount(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        requireOwnership(walletAccount, memberId);
        requireQueryable(walletQueryRepository, walletAccount);
        return walletAccount;
    }

    static void requireOwnership(WalletAccount walletAccount, String memberId) {
        if (!walletAccount.memberId().equals(memberId)) {
            throw new WalletAccessDeniedException(walletAccount.walletId());
        }
    }

    private static void requireQueryable(WalletQueryRepository walletQueryRepository, WalletAccount walletAccount) {
        if (!walletAccount.queryable()) {
            throw new WalletAccountNotQueryableException(walletAccount.walletId());
        }
        Member owner = walletQueryRepository.findMember(walletAccount.memberId())
                .orElseThrow(() -> new WalletAccountNotQueryableException(walletAccount.walletId()));
        if (!owner.active()) {
            throw new WalletAccountNotQueryableException(walletAccount.walletId());
        }
    }
}
