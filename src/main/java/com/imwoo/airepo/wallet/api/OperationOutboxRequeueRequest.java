package com.imwoo.airepo.wallet.api;

public record OperationOutboxRequeueRequest(
        String operator,
        String reason
) {
}
