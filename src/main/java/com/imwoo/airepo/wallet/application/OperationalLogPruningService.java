package com.imwoo.airepo.wallet.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class OperationalLogPruningService {

    private final OperationOutboxRelayRunRepository operationOutboxRelayRunRepository;
    private final AdminApiAccessAuditRepository adminApiAccessAuditRepository;
    private final OperationalAlertRepository operationalAlertRepository;
    private final Clock clock;

    public OperationalLogPruningService(
            OperationOutboxRelayRunRepository operationOutboxRelayRunRepository,
            AdminApiAccessAuditRepository adminApiAccessAuditRepository,
            OperationalAlertRepository operationalAlertRepository,
            Clock clock
    ) {
        this.operationOutboxRelayRunRepository = operationOutboxRelayRunRepository;
        this.adminApiAccessAuditRepository = adminApiAccessAuditRepository;
        this.operationalAlertRepository = operationalAlertRepository;
        this.clock = clock;
    }

    public OperationalLogPruningResult prune(
            Duration relayRunRetention,
            Duration adminAccessAuditRetention,
            Duration operationalAlertRetention
    ) {
        validateRetention("relayRunRetention", relayRunRetention);
        validateRetention("adminAccessAuditRetention", adminAccessAuditRetention);
        validateRetention("operationalAlertRetention", operationalAlertRetention);
        Instant prunedAt = Instant.now(clock);
        Instant relayRunCutoff = prunedAt.minus(relayRunRetention);
        Instant adminAccessAuditCutoff = prunedAt.minus(adminAccessAuditRetention);
        Instant operationalAlertCutoff = prunedAt.minus(operationalAlertRetention);
        int deletedRelayRunCount = operationOutboxRelayRunRepository.deleteOutboxRelayRunsCompletedBefore(
                relayRunCutoff
        );
        int deletedAdminAccessAuditCount = adminApiAccessAuditRepository.deleteAdminApiAccessAuditsOccurredBefore(
                adminAccessAuditCutoff
        );
        int deletedOperationalAlertCount = operationalAlertRepository.deleteOperationalAlertsOccurredBefore(
                operationalAlertCutoff
        );
        return new OperationalLogPruningResult(
                prunedAt,
                relayRunCutoff,
                adminAccessAuditCutoff,
                operationalAlertCutoff,
                deletedRelayRunCount,
                deletedAdminAccessAuditCount,
                deletedOperationalAlertCount
        );
    }

    private void validateRetention(String fieldName, Duration retention) {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new InvalidWalletOperationException(fieldName + " must be positive");
        }
    }
}
