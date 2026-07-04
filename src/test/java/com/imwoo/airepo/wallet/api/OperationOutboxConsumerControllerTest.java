package com.imwoo.airepo.wallet.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imwoo.airepo.AiRepoApplication;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerReceiptRepository;
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

@SpringBootTest(classes = {AiRepoApplication.class, OperationOutboxConsumerControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OperationOutboxConsumerControllerTest {

    private final MockMvc mockMvc;
    private final OperationOutboxConsumerReceiptRepository receiptRepository;

    @Autowired
    OperationOutboxConsumerControllerTest(
            MockMvc mockMvc,
            OperationOutboxConsumerReceiptRepository receiptRepository
    ) {
        this.mockMvc = mockMvc;
        this.receiptRepository = receiptRepository;
    }

    @Test
    void consumesBrokerEventAndTreatsDuplicateAsNoop() throws Exception {
        consumeEvent()
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idempotencyKey").value("outbox-001"))
                .andExpect(jsonPath("$.processed").value(true));

        consumeEvent()
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.idempotencyKey").value("outbox-001"))
                .andExpect(jsonPath("$.processed").value(false));

        org.assertj.core.api.Assertions.assertThat(receiptRepository.findConsumerReceipt("outbox-001"))
                .hasValueSatisfying(receipt -> org.assertj.core.api.Assertions.assertThat(receipt.receivedAt())
                        .isEqualTo(Instant.parse("2026-05-01T00:05:00Z")));
    }

    @Test
    void rejectsHeaderBodyMismatchBeforeSideEffect() throws Exception {
        mockMvc.perform(post("/internal/broker/outbox-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Broker-Token", "local-broker-token")
                        .header("X-Outbox-Event-Id", "outbox-001")
                        .header("X-Idempotency-Key", "outbox-999")
                        .header("X-Event-Schema-Version", "1")
                        .header("X-Event-Type", "CHARGE_COMPLETED")
                        .content(body()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WALLET_OPERATION"));

        org.assertj.core.api.Assertions.assertThat(receiptRepository.findConsumerReceipt("outbox-001")).isEmpty();
    }

    @Test
    void rejectsMissingBrokerTokenWithUnauthorized() throws Exception {
        mockMvc.perform(post("/internal/broker/outbox-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Outbox-Event-Id", "outbox-001")
                        .header("X-Idempotency-Key", "outbox-001")
                        .header("X-Event-Schema-Version", "1")
                        .header("X-Event-Type", "CHARGE_COMPLETED")
                        .content(body()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BROKER_AUTHENTICATION_REQUIRED"));

        org.assertj.core.api.Assertions.assertThat(receiptRepository.findConsumerReceipt("outbox-001")).isEmpty();
    }

    @Test
    void rejectsWrongBrokerTokenWithUnauthorized() throws Exception {
        mockMvc.perform(post("/internal/broker/outbox-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Broker-Token", "wrong-token")
                        .header("X-Outbox-Event-Id", "outbox-001")
                        .header("X-Idempotency-Key", "outbox-001")
                        .header("X-Event-Schema-Version", "1")
                        .header("X-Event-Type", "CHARGE_COMPLETED")
                        .content(body()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BROKER_AUTHENTICATION_REQUIRED"));

        org.assertj.core.api.Assertions.assertThat(receiptRepository.findConsumerReceipt("outbox-001")).isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions consumeEvent() throws Exception {
        return mockMvc.perform(post("/internal/broker/outbox-events")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Broker-Token", "local-broker-token")
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
