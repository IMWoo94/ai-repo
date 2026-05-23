package com.imwoo.airepo.wallet.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = {AiRepoApplication.class, OperationalAlertControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "ai-repo.outbox-consumer.health.min-duplicate-event-count=1",
        "ai-repo.outbox-consumer.health.warning-duplicate-rate-percent=20",
        "ai-repo.outbox-consumer.health.critical-duplicate-rate-percent=50",
        "ai-repo.outbox-consumer.health.window-minutes=5"
})
class OperationalAlertControllerTest {

    private static final String OPERATOR_TOKEN = "local-operator-token";
    private static final String OPERATOR_ID = "ops-user";

    private final MockMvc mockMvc;

    @Autowired
    OperationalAlertControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void returnsOperationalAlertPublishedByConsumerHealth() throws Exception {
        consumeEvent().andExpect(status().isAccepted());
        consumeEvent().andExpect(status().isAccepted());
        mockMvc.perform(get("/api/v1/outbox-consumer/health")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CRITICAL"));

        mockMvc.perform(get("/api/v1/operational-alerts")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].alertId").value("operational-alert-001"))
                .andExpect(jsonPath("$[0].source").value("OUTBOX_CONSUMER"))
                .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                .andExpect(jsonPath("$[0].occurredAt").value("2026-05-01T00:05:00Z"))
                .andExpect(jsonPath("$[0].reasons[0]")
                        .value("critical consumer duplicate delivery rate in health window"));
    }

    @Test
    void rejectsOperationalAlertLookupWithoutOperatorToken() throws Exception {
        mockMvc.perform(get("/api/v1/operational-alerts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsInvalidOperationalAlertLimit() throws Exception {
        mockMvc.perform(get("/api/v1/operational-alerts")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WALLET_OPERATION"))
                .andExpect(jsonPath("$.message").value("limit must be between 1 and 100"));
    }

    @Test
    void recordsOperationalAlertLookupAccessAudit() throws Exception {
        mockMvc.perform(get("/api/v1/operational-alerts")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin-api-access-audits")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].path").value("/api/v1/operational-alerts"))
                .andExpect(jsonPath("$[0].statusCode").value(200))
                .andExpect(jsonPath("$[0].outcome").value("SUCCESS"));
    }

    private org.springframework.test.web.servlet.ResultActions consumeEvent() throws Exception {
        return mockMvc.perform(post("/internal/broker/outbox-events")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Outbox-Event-Id", "outbox-001")
                .header("X-Idempotency-Key", "outbox-001")
                .header("X-Event-Schema-Version", "1")
                .header("X-Event-Type", "CHARGE_COMPLETED")
                .content(body()));
    }

    private String body() {
        return """
                {
                  "schemaVersion": 1,
                  "idempotencyKey": "outbox-001",
                  "outboxEventId": "outbox-001",
                  "operationId": "op-001",
                  "eventType": "CHARGE_COMPLETED",
                  "aggregateType": "WALLET_OPERATION",
                  "aggregateId": "op-001",
                  "occurredAt": "2026-05-01T00:00:00Z",
                  "payload": {
                    "operationId": "op-001",
                    "walletId": "wallet-001"
                  }
                }
                """;
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-05-01T00:05:00Z"), ZoneOffset.UTC);
        }
    }
}
