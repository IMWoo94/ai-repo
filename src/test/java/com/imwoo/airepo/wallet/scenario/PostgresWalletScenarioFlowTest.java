package com.imwoo.airepo.wallet.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imwoo.airepo.AiRepoApplication;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerReceiptRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("postgres-scenario")
@Testcontainers
@ActiveProfiles("postgres")
@SpringBootTest(classes = {AiRepoApplication.class, PostgresWalletScenarioFlowTest.FixedClockConfig.class})
@AutoConfigureMockMvc
class PostgresWalletScenarioFlowTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine")
    )
            .withDatabaseName("ai_repo")
            .withUsername("ai_repo")
            .withPassword("ai_repo");

    private final MockMvc mockMvc;
    private final OperationOutboxRelayService operationOutboxRelayService;
    private final OperationOutboxConsumerReceiptRepository receiptRepository;

    @Autowired
    PostgresWalletScenarioFlowTest(
            MockMvc mockMvc,
            OperationOutboxRelayService operationOutboxRelayService,
            OperationOutboxConsumerReceiptRepository receiptRepository
    ) {
        this.mockMvc = mockMvc;
        this.operationOutboxRelayService = operationOutboxRelayService;
        this.receiptRepository = receiptRepository;
    }

    private static RequestPostProcessor member(String memberId) {
        return jwt().jwt(token -> token.subject(memberId));
    }

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Test
    void postgresProfileMoneyMovementScenarioKeepsLedgerAuditStepAndOutboxEvidence() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/wallet-001/balance").with(member("member-001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value("wallet-001"))
                .andExpect(jsonPath("$.money.amount").value(125000));

        String chargeRequest = """
                {
                  "amount": 5000,
                  "currency": "KRW",
                  "idempotencyKey": "postgres-scenario-charge-001",
                  "description": "PostgreSQL 시나리오 충전"
                }
                """;

        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .with(member("member-001"))
                        .contentType("application/json")
                        .content(chargeRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operationId").value("op-001"))
                .andExpect(jsonPath("$.balance.money.amount").value(130000));

        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .with(member("member-001"))
                        .contentType("application/json")
                        .content(chargeRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationId").value("op-001"))
                .andExpect(jsonPath("$.balance.money.amount").value(130000));

        mockMvc.perform(post("/api/v1/wallets/wallet-001/transfers")
                        .with(member("member-001"))
                        .contentType("application/json")
                        .content("""
                                {
                                  "targetWalletId": "wallet-002",
                                  "amount": 25000,
                                  "currency": "KRW",
                                  "idempotencyKey": "postgres-scenario-transfer-001",
                                  "description": "PostgreSQL 시나리오 송금"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.operationId").value("op-002"))
                .andExpect(jsonPath("$.balance.money.amount").value(105000));

        mockMvc.perform(get("/api/v1/wallets/wallet-001/balance").with(member("member-001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.money.amount").value(105000));
        mockMvc.perform(get("/api/v1/wallets/wallet-002/balance").with(member("member-002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.money.amount").value(55000));
        mockMvc.perform(get("/api/v1/wallets/wallet-001/ledger-entries").with(member("member-001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/v1/audit-events")
                        .header("X-Operator-Token", "local-operator-token")
                        .header("X-Operator-Id", "postgres-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/v1/operations/op-001/step-logs")
                        .header("X-Operator-Token", "local-operator-token")
                        .header("X-Operator-Id", "postgres-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)));
        mockMvc.perform(get("/api/v1/operations/op-002/outbox-events")
                        .header("X-Operator-Token", "local-operator-token")
                        .header("X-Operator-Id", "postgres-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        assertThat(operationOutboxRelayService.publishReadyEvents(10))
                .satisfies(result -> {
                    assertThat(result.claimedCount()).isEqualTo(2);
                    assertThat(result.publishedCount()).isEqualTo(2);
                    assertThat(result.failedCount()).isZero();
                });

        mockMvc.perform(get("/api/v1/operations/op-001/outbox-events")
                        .header("X-Operator-Token", "local-operator-token")
                        .header("X-Operator-Id", "postgres-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
        mockMvc.perform(get("/api/v1/operations/op-002/outbox-events")
                        .header("X-Operator-Token", "local-operator-token")
                        .header("X-Operator-Id", "postgres-ops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));
    }

    @Test
    void postgresProfileConsumesBrokerEventOnceWithTransactionalDedupe() throws Exception {
        consumeBrokerEvent()
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.processed").value(true));

        consumeBrokerEvent()
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.processed").value(false));

        assertThat(receiptRepository.findConsumerReceipt("outbox-001"))
                .hasValueSatisfying(receipt -> {
                    assertThat(receipt.outboxEventId()).isEqualTo("outbox-001");
                    assertThat(receipt.operationId()).isEqualTo("op-001");
                    assertThat(receipt.eventType()).isEqualTo("CHARGE_COMPLETED");
                });
    }

    private org.springframework.test.web.servlet.ResultActions consumeBrokerEvent() throws Exception {
        return mockMvc.perform(post("/internal/broker/outbox-events")
                .contentType("application/json")
                .header("X-Outbox-Event-Id", "outbox-001")
                .header("X-Idempotency-Key", "outbox-001")
                .header("X-Event-Schema-Version", "1")
                .header("X-Event-Type", "CHARGE_COMPLETED")
                .content("""
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
                        """));
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
