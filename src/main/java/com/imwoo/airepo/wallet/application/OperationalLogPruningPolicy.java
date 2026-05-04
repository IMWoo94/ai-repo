package com.imwoo.airepo.wallet.application;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OperationalLogPruningPolicy {

    private final Duration relayRunRetention;
    private final Duration adminAccessAuditRetention;

    public OperationalLogPruningPolicy(
            @Value("${ai-repo.operational-log-pruning.relay-run-retention-days:30}") int relayRunRetentionDays,
            @Value("${ai-repo.operational-log-pruning.admin-access-audit-retention-days:180}") int adminAccessAuditRetentionDays
    ) {
        this.relayRunRetention = retention("relay run retention", relayRunRetentionDays);
        this.adminAccessAuditRetention = retention("admin access audit retention", adminAccessAuditRetentionDays);
    }

    public Duration relayRunRetention() {
        return relayRunRetention;
    }

    public Duration adminAccessAuditRetention() {
        return adminAccessAuditRetention;
    }

    private Duration retention(String name, int days) {
        if (days <= 0) {
            throw new IllegalArgumentException(name + " days must be positive");
        }
        return Duration.ofDays(days);
    }
}
