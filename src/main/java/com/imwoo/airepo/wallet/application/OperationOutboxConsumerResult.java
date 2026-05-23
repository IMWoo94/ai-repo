package com.imwoo.airepo.wallet.application;

public record OperationOutboxConsumerResult(
        String idempotencyKey,
        String outboxEventId,
        String eventType,
        boolean processed
) {
}
