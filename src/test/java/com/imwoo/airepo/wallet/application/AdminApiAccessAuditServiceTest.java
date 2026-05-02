package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.AdminApiAccessOutcome;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AdminApiAccessAuditServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final AdminApiAccessAuditService auditService = new AdminApiAccessAuditService(
            repository,
            Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void recordsSuccessfulAdminApiAccess() {
        auditService.recordAccess("GET", "/api/v1/outbox-relay-runs", " ops-user ", 200);

        assertThat(auditService.getRecentAudits(10))
                .singleElement()
                .satisfies(accessAudit -> {
                    assertThat(accessAudit.auditId()).isEqualTo("admin-api-access-audit-001");
                    assertThat(accessAudit.occurredAt()).isEqualTo(Instant.parse("2026-05-01T00:00:00Z"));
                    assertThat(accessAudit.method()).isEqualTo("GET");
                    assertThat(accessAudit.path()).isEqualTo("/api/v1/outbox-relay-runs");
                    assertThat(accessAudit.operatorId()).isEqualTo("ops-user");
                    assertThat(accessAudit.statusCode()).isEqualTo(200);
                    assertThat(accessAudit.outcome()).isEqualTo(AdminApiAccessOutcome.SUCCESS);
                });
    }

    @Test
    void recordsFailedAdminApiAccessWithoutOperator() {
        auditService.recordAccess("GET", "/api/v1/outbox-relay-runs", null, 401);

        assertThat(auditService.getRecentAudits(10))
                .singleElement()
                .satisfies(accessAudit -> {
                    assertThat(accessAudit.operatorId()).isNull();
                    assertThat(accessAudit.statusCode()).isEqualTo(401);
                    assertThat(accessAudit.outcome()).isEqualTo(AdminApiAccessOutcome.FAILURE);
                });
    }

    @Test
    void rejectsInvalidRecentAuditLimit() {
        assertThatThrownBy(() -> auditService.getRecentAudits(0))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("limit must be positive");
    }
}
