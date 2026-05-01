package com.imwoo.airepo.wallet.application;

public class OperationNotFoundException extends RuntimeException {

    public OperationNotFoundException(String operationId) {
        super("Operation not found: " + operationId);
    }
}
