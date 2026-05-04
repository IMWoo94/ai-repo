package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRun;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRunStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OperationOutboxRelayMonitoringService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 255;

    private final OperationOutboxRelayRunRepository operationOutboxRelayRunRepository;
    private final OutboxRelayHealthPolicy outboxRelayHealthPolicy;
    private final Clock clock;

    public OperationOutboxRelayMonitoringService(
            OperationOutboxRelayRunRepository operationOutboxRelayRunRepository,
            OutboxRelayHealthPolicy outboxRelayHealthPolicy,
            Clock clock
    ) {
        this.operationOutboxRelayRunRepository = operationOutboxRelayRunRepository;
        this.outboxRelayHealthPolicy = outboxRelayHealthPolicy;
        this.clock = clock;
    }

    public void recordSuccess(
            Instant startedAt,
            Instant completedAt,
            int batchSize,
            OperationOutboxPublishBatchResult result
    ) {
        operationOutboxRelayRunRepository.saveOutboxRelayRun(new OperationOutboxRelayRun(
                operationOutboxRelayRunRepository.nextRelayRunId(),
                startedAt,
                completedAt,
                OperationOutboxRelayRunStatus.SUCCESS,
                batchSize,
                result.claimedCount(),
                result.publishedCount(),
                result.failedCount(),
                null
        ));
    }

    public void recordFailure(Instant startedAt, Instant completedAt, int batchSize, String errorMessage) {
        operationOutboxRelayRunRepository.saveOutboxRelayRun(new OperationOutboxRelayRun(
                operationOutboxRelayRunRepository.nextRelayRunId(),
                startedAt,
                completedAt,
                OperationOutboxRelayRunStatus.FAILED,
                batchSize,
                0,
                0,
                0,
                normalizedErrorMessage(errorMessage)
        ));
    }

    public List<OperationOutboxRelayRun> getRecentRuns(int limit) {
        if (limit <= 0) {
            throw new InvalidWalletOperationException("limit must be positive");
        }
        return operationOutboxRelayRunRepository.findRecentOutboxRelayRuns(limit);
    }

    public OutboxRelayHealthSummary getHealthSummary() {
        Instant evaluatedAt = Instant.now(clock);
        List<OperationOutboxRelayRun> recentRuns = operationOutboxRelayRunRepository.findRecentOutboxRelayRuns(
                outboxRelayHealthPolicy.sampleSize()
        );
        if (recentRuns.isEmpty()) {
            return new OutboxRelayHealthSummary(
                    evaluatedAt,
                    OutboxRelayHealthStatus.NO_DATA,
                    outboxRelayHealthPolicy.sampleSize(),
                    0,
                    0,
                    0,
                    0.0,
                    0,
                    null,
                    null,
                    null,
                    List.of("no relay run data")
            );
        }

        int successCount = countStatus(recentRuns, OperationOutboxRelayRunStatus.SUCCESS);
        int failedCount = countStatus(recentRuns, OperationOutboxRelayRunStatus.FAILED);
        double failureRate = (double) failedCount / recentRuns.size();
        int consecutiveFailureCount = consecutiveFailureCount(recentRuns);
        Instant lastCompletedAt = recentRuns.getFirst().completedAt();
        Instant lastSuccessAt = lastCompletedAt(recentRuns, OperationOutboxRelayRunStatus.SUCCESS);
        Instant lastFailureAt = lastCompletedAt(recentRuns, OperationOutboxRelayRunStatus.FAILED);
        List<String> alertReasons = alertReasons(
                evaluatedAt,
                lastSuccessAt,
                consecutiveFailureCount,
                failureRate
        );
        return new OutboxRelayHealthSummary(
                evaluatedAt,
                status(alertReasons),
                outboxRelayHealthPolicy.sampleSize(),
                recentRuns.size(),
                successCount,
                failedCount,
                failureRate,
                consecutiveFailureCount,
                lastCompletedAt,
                lastSuccessAt,
                lastFailureAt,
                alertReasons
        );
    }

    private String normalizedErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Unknown relay failure";
        }
        String trimmedErrorMessage = errorMessage.trim();
        if (trimmedErrorMessage.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return trimmedErrorMessage;
        }
        return trimmedErrorMessage.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }

    private int countStatus(List<OperationOutboxRelayRun> relayRuns, OperationOutboxRelayRunStatus status) {
        return (int) relayRuns.stream()
                .filter(relayRun -> relayRun.status() == status)
                .count();
    }

    private int consecutiveFailureCount(List<OperationOutboxRelayRun> relayRuns) {
        int consecutiveFailureCount = 0;
        for (OperationOutboxRelayRun relayRun : relayRuns) {
            if (relayRun.status() != OperationOutboxRelayRunStatus.FAILED) {
                return consecutiveFailureCount;
            }
            consecutiveFailureCount++;
        }
        return consecutiveFailureCount;
    }

    private Instant lastCompletedAt(List<OperationOutboxRelayRun> relayRuns, OperationOutboxRelayRunStatus status) {
        return relayRuns.stream()
                .filter(relayRun -> relayRun.status() == status)
                .findFirst()
                .map(OperationOutboxRelayRun::completedAt)
                .orElse(null);
    }

    private List<String> alertReasons(
            Instant evaluatedAt,
            Instant lastSuccessAt,
            int consecutiveFailureCount,
            double failureRate
    ) {
        List<String> alertReasons = new ArrayList<>();
        if (consecutiveFailureCount >= outboxRelayHealthPolicy.criticalConsecutiveFailures()) {
            alertReasons.add("critical consecutive relay failures");
        } else if (consecutiveFailureCount >= outboxRelayHealthPolicy.warningConsecutiveFailures()) {
            alertReasons.add("warning consecutive relay failures");
        }
        if (lastSuccessAt == null) {
            alertReasons.add("no successful relay run in recent window");
        } else {
            Duration lastSuccessAge = Duration.between(lastSuccessAt, evaluatedAt);
            if (lastSuccessAge.compareTo(outboxRelayHealthPolicy.criticalLastSuccessAge()) > 0) {
                alertReasons.add("last successful relay run is stale");
            }
        }
        if (failureRate >= outboxRelayHealthPolicy.warningFailureRate()) {
            alertReasons.add("warning relay failure rate");
        }
        return alertReasons;
    }

    private OutboxRelayHealthStatus status(List<String> alertReasons) {
        if (alertReasons.isEmpty()) {
            return OutboxRelayHealthStatus.OK;
        }
        if (alertReasons.stream().anyMatch(alertReason -> alertReason.startsWith("critical")
                || alertReason.equals("last successful relay run is stale")
                || alertReason.equals("no successful relay run in recent window"))) {
            return OutboxRelayHealthStatus.CRITICAL;
        }
        return OutboxRelayHealthStatus.WARNING;
    }
}
