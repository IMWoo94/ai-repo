package com.imwoo.airepo.wallet.domain;

public enum OperationOutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED
}
