package com.imwoo.airepo.wallet.application;

public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException(String idempotencyKey) {
        super("Idempotency key already used for different request: " + idempotencyKey);
    }
}
