package com.imwoo.airepo.wallet.api;

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

@SpringBootTest(classes = {AiRepoApplication.class, WalletCommandControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WalletCommandControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    WalletCommandControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void chargesWallet() throws Exception {
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.walletId").value("wallet-001"))
                .andExpect(jsonPath("$.type").value("CHARGE"))
                .andExpect(jsonPath("$.direction").value("CREDIT"))
                .andExpect(jsonPath("$.balance.money.amount").value(130000));
    }

    @Test
    void returnsOkForDuplicatedChargeWithSameIdempotencyKey() throws Exception {
        String body = """
                {
                  "amount": 5000,
                  "currency": "KRW",
                  "idempotencyKey": "charge-api-001",
                  "description": "API 충전"
                }
                """;

        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance.money.amount").value(130000));
    }

    @Test
    void transfersBetweenWallets() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/wallet-001/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetWalletId": "wallet-002",
                                  "amount": 25000,
                                  "currency": "KRW",
                                  "idempotencyKey": "transfer-api-001",
                                  "description": "API 송금"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.walletId").value("wallet-001"))
                .andExpect(jsonPath("$.counterpartyWalletId").value("wallet-002"))
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.direction").value("DEBIT"))
                .andExpect(jsonPath("$.balance.money.amount").value(100000));
    }

    @Test
    void rejectsInsufficientBalance() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/wallet-002/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetWalletId": "wallet-001",
                                  "amount": 30001,
                                  "currency": "KRW",
                                  "idempotencyKey": "transfer-api-001",
                                  "description": "API 송금"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message").value("Insufficient balance: wallet-002"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-01T00:00:00Z"));
    }

    @Test
    void rejectsNegativeChargeAmountAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": -1,
                                  "currency": "KRW",
                                  "idempotencyKey": "charge-api-001",
                                  "description": "API 충전"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WALLET_OPERATION"))
                .andExpect(jsonPath("$.message").value("amount must be positive"));
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
