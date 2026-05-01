package com.imwoo.airepo.wallet.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imwoo.airepo.AiRepoApplication;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
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

@SpringBootTest(classes = {AiRepoApplication.class, OperationOutboxReviewControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OperationOutboxReviewControllerTest {

    private final MockMvc mockMvc;
    private final OperationOutboxRelayService operationOutboxRelayService;

    @Autowired
    OperationOutboxReviewControllerTest(
            MockMvc mockMvc,
            OperationOutboxRelayService operationOutboxRelayService
    ) {
        this.mockMvc = mockMvc;
        this.operationOutboxRelayService = operationOutboxRelayService;
    }

    @Test
    void returnsManualReviewOutboxEvents() throws Exception {
        makeManualReviewEvent();

        mockMvc.perform(get("/api/v1/outbox-events/manual-review")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].outboxEventId").value("outbox-001"))
                .andExpect(jsonPath("$[0].status").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$[0].attemptCount").value(3))
                .andExpect(jsonPath("$[0].lastError").value("broker unavailable"));
    }

    @Test
    void requeuesManualReviewOutboxEvent() throws Exception {
        makeManualReviewEvent();

        mockMvc.perform(post("/api/v1/outbox-events/outbox-001/requeue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operator": "ops-user",
                                  "reason": "broker recovered"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/outbox-events/manual-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/operations/op-001/outbox-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].attemptCount").value(0))
                .andExpect(jsonPath("$[0].lastError").doesNotExist());
        mockMvc.perform(get("/api/v1/outbox-events/outbox-001/requeue-audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].outboxEventId").value("outbox-001"))
                .andExpect(jsonPath("$[0].operationId").value("op-001"))
                .andExpect(jsonPath("$[0].operator").value("ops-user"))
                .andExpect(jsonPath("$[0].reason").value("broker recovered"));
    }

    @Test
    void rejectsInvalidManualReviewLimit() throws Exception {
        mockMvc.perform(get("/api/v1/outbox-events/manual-review")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WALLET_OPERATION"));
    }

    @Test
    void rejectsInvalidRequeueRequest() throws Exception {
        makeManualReviewEvent();

        mockMvc.perform(post("/api/v1/outbox-events/outbox-001/requeue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operator": " ",
                                  "reason": "broker recovered"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WALLET_OPERATION"));
    }

    private void makeManualReviewEvent() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 5000,
                                  "currency": "KRW",
                                  "idempotencyKey": "charge-api-001",
                                  "description": "API 충전"
                                }
                                """))
                .andExpect(status().isCreated());
        operationOutboxRelayService.markFailed("outbox-001", "broker unavailable");
        operationOutboxRelayService.markFailed("outbox-001", "broker unavailable");
        operationOutboxRelayService.markFailed("outbox-001", "broker unavailable");
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
