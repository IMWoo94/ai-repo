package com.imwoo.airepo.wallet.application;

public class InvalidWalletOperationException extends RuntimeException {

    public InvalidWalletOperationException(String message) {
        super(message);
    }
}
