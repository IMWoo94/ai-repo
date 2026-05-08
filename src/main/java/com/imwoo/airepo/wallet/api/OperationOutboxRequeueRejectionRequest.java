package com.imwoo.airepo.wallet.api;

public record OperationOutboxRequeueRejectionRequest(
        String reason
) {
}
