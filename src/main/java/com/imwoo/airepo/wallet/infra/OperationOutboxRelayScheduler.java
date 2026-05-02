package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationOutboxPublishBatchResult;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayMonitoringService;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "ai-repo.outbox-relay.scheduler",
        name = "enabled",
        havingValue = "true"
)
public class OperationOutboxRelayScheduler {

    private final OperationOutboxRelayService operationOutboxRelayService;
    private final OperationOutboxRelayMonitoringService operationOutboxRelayMonitoringService;
    private final Clock clock;
    private final int batchSize;

    public OperationOutboxRelayScheduler(
            OperationOutboxRelayService operationOutboxRelayService,
            OperationOutboxRelayMonitoringService operationOutboxRelayMonitoringService,
            Clock clock,
            @Value("${ai-repo.outbox-relay.scheduler.batch-size:10}") int batchSize
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("outbox relay scheduler batch-size must be positive");
        }
        this.operationOutboxRelayService = operationOutboxRelayService;
        this.operationOutboxRelayMonitoringService = operationOutboxRelayMonitoringService;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(
            initialDelayString = "${ai-repo.outbox-relay.scheduler.initial-delay-ms:5000}",
            fixedDelayString = "${ai-repo.outbox-relay.scheduler.fixed-delay-ms:5000}"
    )
    public void runScheduled() {
        runOnce();
    }

    public OperationOutboxPublishBatchResult runOnce() {
        Instant startedAt = Instant.now(clock);
        try {
            OperationOutboxPublishBatchResult result = operationOutboxRelayService.publishReadyEvents(batchSize);
            operationOutboxRelayMonitoringService.recordSuccess(startedAt, Instant.now(clock), batchSize, result);
            return result;
        } catch (RuntimeException exception) {
            operationOutboxRelayMonitoringService.recordFailure(
                    startedAt,
                    Instant.now(clock),
                    batchSize,
                    relayFailureMessage(exception)
            );
            throw exception;
        }
    }

    private String relayFailureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}
