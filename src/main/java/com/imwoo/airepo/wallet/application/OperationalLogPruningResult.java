package com.imwoo.airepo.wallet.application;

import java.time.Instant;

public record OperationalLogPruningResult(
        Instant prunedAt,
        Instant relayRunCutoff,
        Instant adminAccessAuditCutoff,
        Instant operationalAlertCutoff,
        int deletedRelayRunCount,
        int deletedAdminAccessAuditCount,
        int deletedOperationalAlertCount
) {
}
