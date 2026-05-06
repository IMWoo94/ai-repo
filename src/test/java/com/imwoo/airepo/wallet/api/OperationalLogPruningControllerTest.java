package com.imwoo.airepo.wallet.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imwoo.airepo.AiRepoApplication;
import com.imwoo.airepo.wallet.application.AdminApiAccessAuditRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayRunRepository;
import com.imwoo.airepo.wallet.domain.AdminApiAccessAudit;
import com.imwoo.airepo.wallet.domain.AdminApiAccessOutcome;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRun;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRunStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = {AiRepoApplication.class, OperationalLogPruningControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "ai-repo.operational-log-pruning.relay-run-retention-days=1",
        "ai-repo.operational-log-pruning.admin-access-audit-retention-days=1"
})
class OperationalLogPruningControllerTest {

    private static final String ADMIN_TOKEN = "local-ops-token";
    private static final String OPERATOR_TOKEN = "local-operator-token";
    private static final String OPERATOR_ID = "ops-user";

    private final MockMvc mockMvc;
    private final OperationOutboxRelayRunRepository relayRunRepository;
    private final AdminApiAccessAuditRepository accessAuditRepository;

    @Autowired
    OperationalLogPruningControllerTest(
            MockMvc mockMvc,
            OperationOutboxRelayRunRepository relayRunRepository,
            AdminApiAccessAuditRepository accessAuditRepository
    ) {
        this.mockMvc = mockMvc;
        this.relayRunRepository = relayRunRepository;
        this.accessAuditRepository = accessAuditRepository;
    }

    @Test
    void prunesOperationalLogsWithAdminAuthorization() throws Exception {
        relayRunRepository.saveOutboxRelayRun(relayRun("outbox-relay-run-001", "2026-04-30T23:59:59Z"));
        relayRunRepository.saveOutboxRelayRun(relayRun("outbox-relay-run-002", "2026-05-01T00:00:00Z"));
        accessAuditRepository.saveAdminApiAccessAudit(accessAudit("admin-api-access-audit-001", "2026-04-30T23:59:59Z"));
        accessAuditRepository.saveAdminApiAccessAudit(accessAudit("admin-api-access-audit-002", "2026-05-01T00:00:00Z"));

        mockMvc.perform(post("/api/v1/operational-log-pruning-runs")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relayRunCutoff").value("2026-05-01T00:00:00Z"))
                .andExpect(jsonPath("$.adminAccessAuditCutoff").value("2026-05-01T00:00:00Z"))
                .andExpect(jsonPath("$.deletedRelayRunCount").value(1))
                .andExpect(jsonPath("$.deletedAdminAccessAuditCount").value(1));
    }

    @Test
    void rejectsOperatorTokenForPruning() throws Exception {
        mockMvc.perform(post("/api/v1/operational-log-pruning-runs")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHORIZATION_DENIED"));
    }

    @Test
    void rejectsMissingAdminToken() throws Exception {
        mockMvc.perform(post("/api/v1/operational-log-pruning-runs")
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
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
                OPERATOR_ID,
                200,
                AdminApiAccessOutcome.SUCCESS
        );
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-05-02T00:00:00Z"), ZoneOffset.UTC);
        }
    }
}
