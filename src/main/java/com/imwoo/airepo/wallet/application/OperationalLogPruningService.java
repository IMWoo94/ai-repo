package com.imwoo.airepo.wallet.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class OperationalLogPruningService {

    private final OperationOutboxRelayRunRepository operationOutboxRelayRunRepository;
    private final AdminApiAccessAuditRepository adminApiAccessAuditRepository;
    private final Clock clock;

    public OperationalLogPruningService(
            OperationOutboxRelayRunRepository operationOutboxRelayRunRepository,
            AdminApiAccessAuditRepository adminApiAccessAuditRepository,
            Clock clock
    ) {
        this.operationOutboxRelayRunRepository = operationOutboxRelayRunRepository;
        this.adminApiAccessAuditRepository = adminApiAccessAuditRepository;
        this.clock = clock;
    }

    public OperationalLogPruningResult prune(
            Duration relayRunRetention,
            Duration adminAccessAuditRetention
    ) {
        validateRetention("relayRunRetention", relayRunRetention);
        validateRetention("adminAccessAuditRetention", adminAccessAuditRetention);
        Instant prunedAt = Instant.now(clock);
        Instant relayRunCutoff = prunedAt.minus(relayRunRetention);
        Instant adminAccessAuditCutoff = prunedAt.minus(adminAccessAuditRetention);
        int deletedRelayRunCount = operationOutboxRelayRunRepository.deleteOutboxRelayRunsCompletedBefore(
                relayRunCutoff
        );
        int deletedAdminAccessAuditCount = adminApiAccessAuditRepository.deleteAdminApiAccessAuditsOccurredBefore(
                adminAccessAuditCutoff
        );
        return new OperationalLogPruningResult(
                prunedAt,
                relayRunCutoff,
                adminAccessAuditCutoff,
                deletedRelayRunCount,
                deletedAdminAccessAuditCount
        );
    }

    private void validateRetention(String fieldName, Duration retention) {
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new InvalidWalletOperationException(fieldName + " must be positive");
        }
    }
}
