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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = {AiRepoApplication.class, TestFixtureControllerTest.FixedClockConfig.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "ai-repo.test-fixtures.enabled=true")
class TestFixtureControllerTest {

    private static final String ADMIN_TOKEN = "local-ops-token";
    private static final String OPERATOR_ID = "ops-user";

    private final MockMvc mockMvc;

    @Autowired
    TestFixtureControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void createsManualReviewOutboxFixtureForE2e() throws Exception {
        mockMvc.perform(post("/api/v1/test-fixtures/outbox-events/manual-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationId").value("op-001"))
                .andExpect(jsonPath("$.outboxEventId").value("outbox-001"));

        mockMvc.perform(get("/api/v1/outbox-events/manual-review")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$[0].lastError").value("e2e broker unavailable"));
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
