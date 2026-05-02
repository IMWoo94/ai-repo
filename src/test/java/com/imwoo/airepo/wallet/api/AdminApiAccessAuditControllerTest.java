package com.imwoo.airepo.wallet.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imwoo.airepo.AiRepoApplication;
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

@SpringBootTest(classes = {AiRepoApplication.class, AdminApiAccessAuditControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminApiAccessAuditControllerTest {

    private static final String ADMIN_TOKEN = "local-ops-token";
    private static final String OPERATOR_ID = "ops-user";

    private final MockMvc mockMvc;

    @Autowired
    AdminApiAccessAuditControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void returnsRecordedSuccessfulAdminApiAccess() throws Exception {
        mockMvc.perform(get("/api/v1/outbox-relay-runs")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin-api-access-audits")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].auditId").value("admin-api-access-audit-001"))
                .andExpect(jsonPath("$[0].method").value("GET"))
                .andExpect(jsonPath("$[0].path").value("/api/v1/outbox-relay-runs"))
                .andExpect(jsonPath("$[0].operatorId").value(OPERATOR_ID))
                .andExpect(jsonPath("$[0].statusCode").value(200))
                .andExpect(jsonPath("$[0].outcome").value("SUCCESS"));
    }

    @Test
    void recordsFailedAdminApiAccess() throws Exception {
        mockMvc.perform(get("/api/v1/outbox-relay-runs")
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));

        mockMvc.perform(get("/api/v1/admin-api-access-audits")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].path").value("/api/v1/outbox-relay-runs"))
                .andExpect(jsonPath("$[0].operatorId").value(OPERATOR_ID))
                .andExpect(jsonPath("$[0].statusCode").value(401))
                .andExpect(jsonPath("$[0].outcome").value("FAILURE"));
    }

    @Test
    void rejectsMissingAdminTokenOnAuditEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin-api-access-audits")
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
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
