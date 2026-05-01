package com.imwoo.airepo.wallet.application;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String walletId) {
        super("Insufficient balance: " + walletId);
    }
}
