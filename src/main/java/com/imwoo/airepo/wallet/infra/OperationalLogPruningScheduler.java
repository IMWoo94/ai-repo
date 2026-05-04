package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationalLogPruningPolicy;
import com.imwoo.airepo.wallet.application.OperationalLogPruningResult;
import com.imwoo.airepo.wallet.application.OperationalLogPruningService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "ai-repo.operational-log-pruning.scheduler",
        name = "enabled",
        havingValue = "true"
)
public class OperationalLogPruningScheduler {

    private final OperationalLogPruningService operationalLogPruningService;
    private final OperationalLogPruningPolicy operationalLogPruningPolicy;

    public OperationalLogPruningScheduler(
            OperationalLogPruningService operationalLogPruningService,
            OperationalLogPruningPolicy operationalLogPruningPolicy
    ) {
        this.operationalLogPruningService = operationalLogPruningService;
        this.operationalLogPruningPolicy = operationalLogPruningPolicy;
    }

    @Scheduled(
            initialDelayString = "${ai-repo.operational-log-pruning.scheduler.initial-delay-ms:60000}",
            fixedDelayString = "${ai-repo.operational-log-pruning.scheduler.fixed-delay-ms:86400000}"
    )
    public void runScheduled() {
        runOnce();
    }

    public OperationalLogPruningResult runOnce() {
        return operationalLogPruningService.prune(
                operationalLogPruningPolicy.relayRunRetention(),
                operationalLogPruningPolicy.adminAccessAuditRetention()
        );
    }
}
