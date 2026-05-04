package com.imwoo.airepo.wallet.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imwoo.airepo.AiRepoApplication;
import com.imwoo.airepo.wallet.application.OperationOutboxPublishBatchResult;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayMonitoringService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = {AiRepoApplication.class, OperationOutboxRelayRunControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OperationOutboxRelayRunControllerTest {

    private static final String ADMIN_TOKEN = "local-ops-token";
    private static final String OPERATOR_ID = "ops-user";

    private final MockMvc mockMvc;
    private final OperationOutboxRelayMonitoringService monitoringService;

    @Autowired
    OperationOutboxRelayRunControllerTest(
            MockMvc mockMvc,
            OperationOutboxRelayMonitoringService monitoringService
    ) {
        this.mockMvc = mockMvc;
        this.monitoringService = monitoringService;
    }

    @Test
    void returnsRecentRelayRuns() throws Exception {
        monitoringService.recordSuccess(
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:01Z"),
                10,
                new OperationOutboxPublishBatchResult(3, 2, 1)
        );

        mockMvc.perform(get("/api/v1/outbox-relay-runs")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].relayRunId").value("outbox-relay-run-001"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].batchSize").value(10))
                .andExpect(jsonPath("$[0].claimedCount").value(3))
                .andExpect(jsonPath("$[0].publishedCount").value(2))
                .andExpect(jsonPath("$[0].failedCount").value(1))
                .andExpect(jsonPath("$[0].errorMessage").doesNotExist());
    }

    @Test
    void returnsRelayHealthSummary() throws Exception {
        monitoringService.recordSuccess(
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:01Z"),
                10,
                new OperationOutboxPublishBatchResult(3, 2, 1)
        );

        mockMvc.perform(get("/api/v1/outbox-relay-runs/health")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.totalRunCount").value(1))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.lastSuccessAt").value("2026-05-01T00:00:01Z"));
    }

    @Test
    void rejectsMissingAdminToken() throws Exception {
        mockMvc.perform(get("/api/v1/outbox-relay-runs")
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsInvalidLimit() throws Exception {
        mockMvc.perform(get("/api/v1/outbox-relay-runs")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WALLET_OPERATION"));
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
        }
    }
}
