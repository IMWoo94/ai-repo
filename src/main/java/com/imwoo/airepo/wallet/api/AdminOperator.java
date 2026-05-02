package com.imwoo.airepo.wallet.api;

public record AdminOperator(String operatorId) {

    public AdminOperator {
        if (operatorId == null || operatorId.isBlank()) {
            throw new IllegalArgumentException("operatorId must not be blank");
        }
    }
}
