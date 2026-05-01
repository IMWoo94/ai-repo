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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = {AiRepoApplication.class, WalletQueryControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
class WalletQueryControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    WalletQueryControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void returnsBalance() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/wallet-001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value("wallet-001"))
                .andExpect(jsonPath("$.money.amount").value(125000))
                .andExpect(jsonPath("$.money.currency").value("KRW"))
                .andExpect(jsonPath("$.asOf").value("2026-05-01T00:00:00Z"));
    }

    @Test
    void returnsTransactionHistoryByLatestFirst() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/wallet-001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].transactionId").value("txn-002"))
                .andExpect(jsonPath("$[1].transactionId").value("txn-001"));
    }

    @Test
    void returnsNotFoundForUnknownWallet() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/unknown/balance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Wallet not found: unknown"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-01T00:00:00Z"));
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
