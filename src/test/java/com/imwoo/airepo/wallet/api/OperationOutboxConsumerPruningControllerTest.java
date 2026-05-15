package com.imwoo.airepo.wallet.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imwoo.airepo.AiRepoApplication;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerIdempotencyRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerReceiptRepository;
import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = {AiRepoApplication.class, OperationOutboxConsumerPruningControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "ai-repo.outbox-consumer-pruning.processed-event-retention-days=1",
        "ai-repo.outbox-consumer-pruning.receipt-retention-days=1"
})
class OperationOutboxConsumerPruningControllerTest {

    private static final String ADMIN_TOKEN = "local-ops-token";
    private static final String OPERATOR_TOKEN = "local-operator-token";
    private static final String OPERATOR_ID = "ops-user";

    private final MockMvc mockMvc;
    private final OperationOutboxConsumerIdempotencyRepository idempotencyRepository;
    private final OperationOutboxConsumerReceiptRepository receiptRepository;

    @Autowired
    OperationOutboxConsumerPruningControllerTest(
            MockMvc mockMvc,
            OperationOutboxConsumerIdempotencyRepository idempotencyRepository,
            OperationOutboxConsumerReceiptRepository receiptRepository
    ) {
        this.mockMvc = mockMvc;
        this.idempotencyRepository = idempotencyRepository;
        this.receiptRepository = receiptRepository;
    }

    @Test
    void prunesConsumerDedupeRecordsWithAdminAuthorization() throws Exception {
        idempotencyRepository.recordProcessedEvent(
                "outbox-001",
                "outbox-001",
                "CHARGE_COMPLETED",
                Instant.parse("2026-04-30T23:59:59Z")
        );
        idempotencyRepository.recordProcessedEvent(
                "outbox-002",
                "outbox-002",
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:00:00Z")
        );
        receiptRepository.saveConsumerReceipt(receipt("outbox-001", "2026-04-30T23:59:59Z"));
        receiptRepository.saveConsumerReceipt(receipt("outbox-002", "2026-05-01T00:00:00Z"));

        mockMvc.perform(post("/api/v1/outbox-consumer/pruning-runs")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedEventCutoff").value("2026-05-01T00:00:00Z"))
                .andExpect(jsonPath("$.receiptCutoff").value("2026-05-01T00:00:00Z"))
                .andExpect(jsonPath("$.deletedProcessedEventCount").value(1))
                .andExpect(jsonPath("$.deletedReceiptCount").value(1));
    }

    @Test
    void rejectsOperatorTokenForConsumerPruning() throws Exception {
        mockMvc.perform(post("/api/v1/outbox-consumer/pruning-runs")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHORIZATION_DENIED"));
    }

    @Test
    void rejectsMissingAdminTokenForConsumerPruning() throws Exception {
        mockMvc.perform(post("/api/v1/outbox-consumer/pruning-runs")
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
    }

    private OperationOutboxConsumerReceipt receipt(String idempotencyKey, String receivedAt) {
        return new OperationOutboxConsumerReceipt(
                idempotencyKey,
                idempotencyKey,
                "op-" + idempotencyKey.substring(idempotencyKey.length() - 3),
                "CHARGE_COMPLETED",
                "WALLET_OPERATION",
                "op-" + idempotencyKey.substring(idempotencyKey.length() - 3),
                Instant.parse(receivedAt)
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
