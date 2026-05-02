package com.imwoo.airepo.wallet.api;

public record OperationOutboxRequeueRequest(
        String reason
) {
}
