package com.imwoo.airepo.wallet.domain;

public enum OperationStep {
    BALANCE_LOCKED,
    BALANCE_UPDATED,
    TRANSACTION_RECORDED,
    LEDGER_RECORDED,
    AUDIT_RECORDED,
    IDEMPOTENCY_RECORDED
}
