package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.Member;
import com.imwoo.airepo.wallet.domain.WalletAccount;

final class WalletAccessPolicy {

    private WalletAccessPolicy() {
    }

    static WalletAccount findQueryableWallet(WalletQueryRepository walletQueryRepository, String walletId) {
        WalletAccount walletAccount = walletQueryRepository.findWalletAccount(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
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
}
