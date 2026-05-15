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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = {AiRepoApplication.class, OperationOutboxConsumerMonitoringControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OperationOutboxConsumerMonitoringControllerTest {

    private static final String OPERATOR_TOKEN = "local-operator-token";
    private static final String OPERATOR_ID = "ops-user";

    private final MockMvc mockMvc;

    @Autowired
    OperationOutboxConsumerMonitoringControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void returnsConsumerMetricsAndRecentReceipts() throws Exception {
        consumeEvent().andExpect(status().isAccepted());
        consumeEvent().andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/outbox-consumer/metrics")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedEventCount").value(1))
                .andExpect(jsonPath("$.duplicateEventCount").value(1))
                .andExpect(jsonPath("$.receiptCount").value(1))
                .andExpect(jsonPath("$.lastProcessedAt").value("2026-05-01T00:05:00Z"))
                .andExpect(jsonPath("$.lastReceivedAt").value("2026-05-01T00:05:00Z"));

        mockMvc.perform(get("/api/v1/outbox-consumer/receipts")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idempotencyKey").value("outbox-001"))
                .andExpect(jsonPath("$[0].operationId").value("op-001"))
                .andExpect(jsonPath("$[0].eventType").value("CHARGE_COMPLETED"));
    }

    @Test
    void rejectsConsumerMonitoringWithoutOperatorToken() throws Exception {
        mockMvc.perform(get("/api/v1/outbox-consumer/metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
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
