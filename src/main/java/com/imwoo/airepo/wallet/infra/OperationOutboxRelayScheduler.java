package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationOutboxPublishBatchResult;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
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
    private final int batchSize;

    public OperationOutboxRelayScheduler(
            OperationOutboxRelayService operationOutboxRelayService,
            @Value("${ai-repo.outbox-relay.scheduler.batch-size:10}") int batchSize
    ) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("outbox relay scheduler batch-size must be positive");
        }
        this.operationOutboxRelayService = operationOutboxRelayService;
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
        return operationOutboxRelayService.publishReadyEvents(batchSize);
    }
}
