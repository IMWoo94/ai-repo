package com.imwoo.airepo.wallet.domain;

import java.time.Instant;
import java.util.Objects;

public record OperationOutboxRelayRun(
        String relayRunId,
        Instant startedAt,
        Instant completedAt,
        OperationOutboxRelayRunStatus status,
        int batchSize,
        int claimedCount,
        int publishedCount,
        int failedCount,
        String errorMessage
) {

    public OperationOutboxRelayRun {
        Objects.requireNonNull(relayRunId, "relayRunId must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        Objects.requireNonNull(completedAt, "completedAt must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (relayRunId.isBlank()) {
            throw new IllegalArgumentException("relayRunId must not be blank");
        }
        if (completedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("completedAt must not be before startedAt");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        if (claimedCount < 0) {
            throw new IllegalArgumentException("claimedCount must not be negative");
        }
        if (publishedCount < 0) {
            throw new IllegalArgumentException("publishedCount must not be negative");
        }
        if (failedCount < 0) {
            throw new IllegalArgumentException("failedCount must not be negative");
        }
        if (status == OperationOutboxRelayRunStatus.SUCCESS && errorMessage != null) {
            throw new IllegalArgumentException("errorMessage must be null when status is SUCCESS");
        }
        if (status == OperationOutboxRelayRunStatus.FAILED && (errorMessage == null || errorMessage.isBlank())) {
            throw new IllegalArgumentException("errorMessage must not be blank when status is FAILED");
        }
    }
}
