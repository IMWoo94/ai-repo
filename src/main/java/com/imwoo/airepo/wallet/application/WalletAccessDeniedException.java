package com.imwoo.airepo.wallet.application;

public class WalletAccessDeniedException extends RuntimeException {

    public WalletAccessDeniedException(String walletId) {
        super("Wallet access denied: " + walletId);
    }
}
