package com.imwoo.airepo.wallet.application;

public class WalletAccountNotQueryableException extends RuntimeException {

    public WalletAccountNotQueryableException(String walletId) {
        super("Wallet is not queryable: " + walletId);
    }
}
