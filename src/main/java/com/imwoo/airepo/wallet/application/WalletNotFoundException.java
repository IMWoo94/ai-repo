package com.imwoo.airepo.wallet.application;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(String walletId) {
        super("Wallet not found: " + walletId);
    }
}
