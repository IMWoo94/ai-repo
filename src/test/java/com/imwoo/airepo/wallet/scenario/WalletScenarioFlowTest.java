package com.imwoo.airepo.wallet.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imwoo.airepo.AiRepoApplication;
import com.imwoo.airepo.wallet.api.AdminAuthorizationGuard;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Tag;
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

@Tag("scenario")
@SpringBootTest(classes = {AiRepoApplication.class, WalletScenarioFlowTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WalletScenarioFlowTest {

    private static final String ADMIN_TOKEN = "local-ops-token";
    private static final String OPERATOR_ID = "scenario-ops";

    private final MockMvc mockMvc;
    private final OperationOutboxRelayService operationOutboxRelayService;

    @Autowired
    WalletScenarioFlowTest(MockMvc mockMvc, OperationOutboxRelayService operationOutboxRelayService) {
        this.mockMvc = mockMvc;
        this.operationOutboxRelayService = operationOutboxRelayService;
    }

    @Test
    void moneyMovementScenarioCreatesUserLedgerAuditStepAndOutboxEvidence() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/wallet-001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value("wallet-001"))
                .andExpect(jsonPath("$.money.amount").value(125000));

        String chargeRequest = """
                {
                  "amount": 5000,
                  "currency": "KRW",
                  "idempotencyKey": "scenario-charge-001",
                  "description": "시나리오 충전"
                }
                """;

        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chargeRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operationId").value("op-001"))
                .andExpect(jsonPath("$.type").value("CHARGE"))
                .andExpect(jsonPath("$.direction").value("CREDIT"))
                .andExpect(jsonPath("$.balance.money.amount").value(130000));

        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chargeRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationId").value("op-001"))
                .andExpect(jsonPath("$.balance.money.amount").value(130000));

        mockMvc.perform(post("/api/v1/wallets/wallet-001/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetWalletId": "wallet-002",
                                  "amount": 25000,
                                  "currency": "KRW",
                                  "idempotencyKey": "scenario-transfer-001",
                                  "description": "시나리오 송금"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operationId").value("op-002"))
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.direction").value("DEBIT"))
                .andExpect(jsonPath("$.balance.money.amount").value(105000));

        mockMvc.perform(get("/api/v1/wallets/wallet-001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.money.amount").value(105000));
        mockMvc.perform(get("/api/v1/wallets/wallet-002/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.money.amount").value(55000));
        mockMvc.perform(get("/api/v1/wallets/wallet-001/ledger-entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/v1/audit-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("TRANSFER_COMPLETED"))
                .andExpect(jsonPath("$[1].type").value("CHARGE_COMPLETED"));
        mockMvc.perform(get("/api/v1/operations/op-001/step-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].step").value("BALANCE_LOCKED"))
                .andExpect(jsonPath("$[5].step").value("IDEMPOTENCY_RECORDED"));
        mockMvc.perform(get("/api/v1/operations/op-002/outbox-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].outboxEventId").value("outbox-002"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void outboxOperationsScenarioMovesManualReviewEventBackToPendingWithAuditTrail() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 5000,
                                  "currency": "KRW",
                                  "idempotencyKey": "scenario-outbox-charge-001",
                                  "description": "시나리오 outbox 충전"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operationId").value("op-001"));

        operationOutboxRelayService.markFailed("outbox-001", "broker unavailable");
        operationOutboxRelayService.markFailed("outbox-001", "broker unavailable");
        operationOutboxRelayService.markFailed("outbox-001", "broker unavailable");

        mockMvc.perform(get("/api/v1/outbox-events/manual-review")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].outboxEventId").value("outbox-001"))
                .andExpect(jsonPath("$[0].status").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$[0].attemptCount").value(3));

        mockMvc.perform(post("/api/v1/outbox-events/outbox-001/requeue")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "broker recovered in scenario"
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/outbox-events/outbox-001/requeue-audits")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].outboxEventId").value("outbox-001"))
                .andExpect(jsonPath("$[0].operationId").value("op-001"))
                .andExpect(jsonPath("$[0].operator").value("scenario-ops"))
                .andExpect(jsonPath("$[0].reason").value("broker recovered in scenario"));

        mockMvc.perform(get("/api/v1/operations/op-001/outbox-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].attemptCount").value(0));
    }

    @Test
    void outboxPublishScenarioMovesReadyEventToPublished() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 5000,
                                  "currency": "KRW",
                                  "idempotencyKey": "scenario-outbox-publish-001",
                                  "description": "시나리오 outbox 발행"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operationId").value("op-001"));

        assertThat(operationOutboxRelayService.publishReadyEvents(10))
                .satisfies(result -> {
                    assertThat(result.claimedCount()).isEqualTo(1);
                    assertThat(result.publishedCount()).isEqualTo(1);
                    assertThat(result.failedCount()).isZero();
                });

        mockMvc.perform(get("/api/v1/operations/op-001/outbox-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].outboxEventId").value("outbox-001"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$[0].publishedAt").value("2026-05-01T00:00:00Z"));
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
