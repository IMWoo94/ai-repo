package com.imwoo.airepo.wallet.application;

public class InvalidWalletIdException extends RuntimeException {

    public InvalidWalletIdException(String message) {
        super(message);
    }
}
