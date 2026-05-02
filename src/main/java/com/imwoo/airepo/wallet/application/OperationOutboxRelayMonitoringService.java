package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRun;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRunStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OperationOutboxRelayMonitoringService {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 255;

    private final OperationOutboxRelayRunRepository operationOutboxRelayRunRepository;

    public OperationOutboxRelayMonitoringService(OperationOutboxRelayRunRepository operationOutboxRelayRunRepository) {
        this.operationOutboxRelayRunRepository = operationOutboxRelayRunRepository;
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
}
