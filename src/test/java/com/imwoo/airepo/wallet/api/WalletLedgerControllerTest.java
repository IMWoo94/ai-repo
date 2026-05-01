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

@SpringBootTest(classes = {AiRepoApplication.class, WalletLedgerControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WalletLedgerControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    WalletLedgerControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void returnsLedgerEntriesAfterCharge() throws Exception {
        charge();

        mockMvc.perform(get("/api/v1/wallets/wallet-001/ledger-entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].operationId").value("op-001"))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[0].direction").value("CREDIT"))
                .andExpect(jsonPath("$[0].balanceAfter.amount").value(130000));
    }

    @Test
    void returnsAuditEventsAfterCharge() throws Exception {
        charge();

        mockMvc.perform(get("/api/v1/audit-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].operationId").value("op-001"))
                .andExpect(jsonPath("$[0].type").value("CHARGE_COMPLETED"));
    }

    @Test
    void returnsOperationStepLogsAfterCharge() throws Exception {
        charge();

        mockMvc.perform(get("/api/v1/operations/op-001/step-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].operationId").value("op-001"))
                .andExpect(jsonPath("$[0].step").value("BALANCE_LOCKED"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[5].step").value("IDEMPOTENCY_RECORDED"));
    }

    @Test
    void returnsOperationOutboxEventsAfterCharge() throws Exception {
        charge();

        mockMvc.perform(get("/api/v1/operations/op-001/outbox-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].operationId").value("op-001"))
                .andExpect(jsonPath("$[0].eventType").value("CHARGE_COMPLETED"))
                .andExpect(jsonPath("$[0].aggregateType").value("WALLET_OPERATION"))
                .andExpect(jsonPath("$[0].aggregateId").value("op-001"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void rejectsUnknownOperationStepLogQuery() throws Exception {
        mockMvc.perform(get("/api/v1/operations/op-9999/step-logs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("OPERATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Operation not found: op-9999"));
    }

    @Test
    void rejectsUnknownOperationOutboxEventQuery() throws Exception {
        mockMvc.perform(get("/api/v1/operations/op-9999/outbox-events"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("OPERATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Operation not found: op-9999"));
    }

    private void charge() throws Exception {
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
