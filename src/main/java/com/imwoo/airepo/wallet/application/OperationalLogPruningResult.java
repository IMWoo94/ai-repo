package com.imwoo.airepo.wallet.application;

import java.time.Instant;

public record OperationalLogPruningResult(
        Instant prunedAt,
        Instant relayRunCutoff,
        Instant adminAccessAuditCutoff,
        int deletedRelayRunCount,
        int deletedAdminAccessAuditCount
) {
}
