package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationOutboxConsumerPruningPolicy;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerPruningResult;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerPruningService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "ai-repo.outbox-consumer-pruning.scheduler",
        name = "enabled",
        havingValue = "true"
)
public class OperationOutboxConsumerPruningScheduler {

    private final OperationOutboxConsumerPruningService pruningService;
    private final OperationOutboxConsumerPruningPolicy pruningPolicy;

    public OperationOutboxConsumerPruningScheduler(
            OperationOutboxConsumerPruningService pruningService,
            OperationOutboxConsumerPruningPolicy pruningPolicy
    ) {
        this.pruningService = pruningService;
        this.pruningPolicy = pruningPolicy;
    }

    @Scheduled(
            initialDelayString = "${ai-repo.outbox-consumer-pruning.scheduler.initial-delay-ms:60000}",
            fixedDelayString = "${ai-repo.outbox-consumer-pruning.scheduler.fixed-delay-ms:86400000}"
    )
    public void runScheduled() {
        runOnce();
    }

    public OperationOutboxConsumerPruningResult runOnce() {
        return pruningService.prune(
                pruningPolicy.processedEventRetention(),
                pruningPolicy.receiptRetention()
        );
    }
}
