package com.imwoo.airepo.wallet.application;

public record OperationOutboxPublishBatchResult(
        int claimedCount,
        int publishedCount,
        int failedCount
) {

    public OperationOutboxPublishBatchResult {
        if (claimedCount < 0) {
            throw new IllegalArgumentException("claimedCount must not be negative");
        }
        if (publishedCount < 0) {
            throw new IllegalArgumentException("publishedCount must not be negative");
        }
        if (failedCount < 0) {
            throw new IllegalArgumentException("failedCount must not be negative");
        }
        if (claimedCount != publishedCount + failedCount) {
            throw new IllegalArgumentException("claimedCount must equal publishedCount plus failedCount");
        }
    }
}
