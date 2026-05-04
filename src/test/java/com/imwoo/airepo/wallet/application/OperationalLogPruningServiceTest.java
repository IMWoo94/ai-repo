package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.AdminApiAccessAudit;
import com.imwoo.airepo.wallet.domain.AdminApiAccessOutcome;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRun;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRunStatus;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OperationalLogPruningServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final OperationalLogPruningService pruningService = new OperationalLogPruningService(
            repository,
            repository,
            Clock.fixed(Instant.parse("2026-05-02T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void prunesOperationalLogsOlderThanRetentionCutoff() {
        repository.saveOutboxRelayRun(relayRun("outbox-relay-run-001", "2026-04-30T23:59:59Z"));
        repository.saveOutboxRelayRun(relayRun("outbox-relay-run-002", "2026-05-01T00:00:00Z"));
        repository.saveAdminApiAccessAudit(accessAudit("admin-api-access-audit-001", "2026-04-30T23:59:59Z"));
        repository.saveAdminApiAccessAudit(accessAudit("admin-api-access-audit-002", "2026-05-01T00:00:00Z"));

        OperationalLogPruningResult result = pruningService.prune(Duration.ofDays(1), Duration.ofDays(1));

        assertThat(result.prunedAt()).isEqualTo(Instant.parse("2026-05-02T00:00:00Z"));
        assertThat(result.relayRunCutoff()).isEqualTo(Instant.parse("2026-05-01T00:00:00Z"));
        assertThat(result.adminAccessAuditCutoff()).isEqualTo(Instant.parse("2026-05-01T00:00:00Z"));
        assertThat(result.deletedRelayRunCount()).isEqualTo(1);
        assertThat(result.deletedAdminAccessAuditCount()).isEqualTo(1);
        assertThat(repository.findRecentOutboxRelayRuns(10))
                .singleElement()
                .satisfies(relayRun -> assertThat(relayRun.relayRunId()).isEqualTo("outbox-relay-run-002"));
        assertThat(repository.findRecentAdminApiAccessAudits(10))
                .singleElement()
                .satisfies(accessAudit -> assertThat(accessAudit.auditId()).isEqualTo("admin-api-access-audit-002"));
    }

    @Test
    void rejectsInvalidRetention() {
        assertThatThrownBy(() -> pruningService.prune(Duration.ZERO, Duration.ofDays(1)))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("relayRunRetention must be positive");
        assertThatThrownBy(() -> pruningService.prune(Duration.ofDays(1), Duration.ZERO))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("adminAccessAuditRetention must be positive");
    }

    private OperationOutboxRelayRun relayRun(String relayRunId, String completedAt) {
        return new OperationOutboxRelayRun(
                relayRunId,
                Instant.parse(completedAt).minusSeconds(1),
                Instant.parse(completedAt),
                OperationOutboxRelayRunStatus.SUCCESS,
                10,
                0,
                0,
                0,
                null
        );
    }

    private AdminApiAccessAudit accessAudit(String auditId, String occurredAt) {
        return new AdminApiAccessAudit(
                auditId,
                Instant.parse(occurredAt),
                "GET",
                "/api/v1/outbox-relay-runs",
                "ops-user",
                200,
                AdminApiAccessOutcome.SUCCESS
        );
    }
}
