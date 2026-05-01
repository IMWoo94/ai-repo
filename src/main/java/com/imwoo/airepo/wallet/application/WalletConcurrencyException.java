package com.imwoo.airepo.wallet.application;

public class WalletConcurrencyException extends RuntimeException {

    public WalletConcurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
