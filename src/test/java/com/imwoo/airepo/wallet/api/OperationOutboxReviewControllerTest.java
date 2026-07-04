package com.imwoo.airepo.wallet.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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

    private static final String ADMIN_TOKEN = "local-ops-token";
    private static final String OPERATOR_TOKEN = "local-operator-token";
    private static final String OPERATOR_ID = "ops-user";

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
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].outboxEventId").value("outbox-001"))
                .andExpect(jsonPath("$[0].status").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$[0].attemptCount").value(3))
                .andExpect(jsonPath("$[0].lastError").value("broker unavailable"));
    }

    @Test
    void rejectsOperatorTokenForRequeueAction() throws Exception {
        makeManualReviewEvent();

        mockMvc.perform(post("/api/v1/outbox-events/outbox-001/requeue")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "broker recovered"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHORIZATION_DENIED"));
    }

    @Test
    void rejectsDeprecatedDirectRequeueApi() throws Exception {
        makeManualReviewEvent();

        mockMvc.perform(post("/api/v1/outbox-events/outbox-001/requeue")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, "ops-header-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "broker recovered"
                                }
                                """))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("DIRECT_REQUEUE_API_DEPRECATED"));

        mockMvc.perform(get("/api/v1/outbox-events/manual-review")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("MANUAL_REVIEW"));
        mockMvc.perform(get("/api/v1/operations/op-001/outbox-events")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("MANUAL_REVIEW"))
                .andExpect(jsonPath("$[0].attemptCount").value(3))
                .andExpect(jsonPath("$[0].lastError").value("broker unavailable"));
        mockMvc.perform(get("/api/v1/outbox-events/outbox-001/requeue-audits")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void requestsApprovesAndExecutesManualReviewRequeue() throws Exception {
        makeManualReviewEvent();

        mockMvc.perform(post("/api/v1/outbox-events/outbox-001/requeue-requests")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "broker recovered"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("outbox-requeue-request-001"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.requestedBy").value(OPERATOR_ID))
                .andExpect(jsonPath("$.requestReason").value("broker recovered"));

        mockMvc.perform(post("/api/v1/outbox-events/requeue-requests/outbox-requeue-request-001/approve")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, "ops-approver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "원인 조치 확인"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHORIZATION_DENIED"));

        mockMvc.perform(post("/api/v1/outbox-events/requeue-requests/outbox-requeue-request-001/approve")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, "ops-approver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "원인 조치 확인"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.approvedBy").value("ops-approver"))
                .andExpect(jsonPath("$.approvalReason").value("원인 조치 확인"));

        mockMvc.perform(post("/api/v1/outbox-events/requeue-requests/outbox-requeue-request-001/execute")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, "ops-executor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.executedBy").value("ops-executor"));

        mockMvc.perform(get("/api/v1/outbox-events/outbox-001/requeue-requests")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("EXECUTED"));
        mockMvc.perform(get("/api/v1/operations/op-001/outbox-events")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
        mockMvc.perform(get("/api/v1/outbox-events/outbox-001/requeue-audits")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].operator").value("ops-executor"))
                .andExpect(jsonPath("$[0].reason").value("broker recovered"));
    }

    @Test
    void rejectsSameRequesterApproverForRequeueRequest() throws Exception {
        makeManualReviewEvent();

        mockMvc.perform(post("/api/v1/outbox-events/outbox-001/requeue-requests")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "broker recovered"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/outbox-events/requeue-requests/outbox-requeue-request-001/approve")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "self approval"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WALLET_OPERATION"));
    }

    @Test
    void rejectsManualReviewRequeueRequest() throws Exception {
        makeManualReviewEvent();

        mockMvc.perform(post("/api/v1/outbox-events/outbox-001/requeue-requests")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "broker recovered"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/outbox-events/requeue-requests/outbox-requeue-request-001/reject")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, "ops-rejector")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "원인 조치 미확인"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHORIZATION_DENIED"));

        mockMvc.perform(post("/api/v1/outbox-events/requeue-requests/outbox-requeue-request-001/reject")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, "ops-rejector")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "원인 조치 미확인"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectedBy").value("ops-rejector"))
                .andExpect(jsonPath("$.rejectionReason").value("원인 조치 미확인"));

        mockMvc.perform(get("/api/v1/operations/op-001/outbox-events")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("MANUAL_REVIEW"));
        mockMvc.perform(get("/api/v1/outbox-events/outbox-001/requeue-audits")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsSameRequesterRejectorForRequeueRequest() throws Exception {
        makeManualReviewEvent();

        mockMvc.perform(post("/api/v1/outbox-events/outbox-001/requeue-requests")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, OPERATOR_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "broker recovered"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/outbox-events/requeue-requests/outbox-requeue-request-001/reject")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "self rejection"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WALLET_OPERATION"));
    }

    @Test
    void rejectsInvalidManualReviewLimit() throws Exception {
        mockMvc.perform(get("/api/v1/outbox-events/manual-review")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN)
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID)
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_WALLET_OPERATION"));
    }

    @Test
    void rejectsUnauthenticatedManualReviewRead() throws Exception {
        makeManualReviewEvent();

        mockMvc.perform(get("/api/v1/outbox-events/manual-review"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsMissingAdminToken() throws Exception {
        mockMvc.perform(get("/api/v1/outbox-events/manual-review")
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsInvalidAdminToken() throws Exception {
        mockMvc.perform(get("/api/v1/outbox-events/manual-review")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, "wrong-token")
                        .header(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, "wrong-token")
                        .header(AdminAuthorizationGuard.OPERATOR_ID_HEADER, OPERATOR_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsMissingOperatorId() throws Exception {
        mockMvc.perform(get("/api/v1/outbox-events/manual-review")
                        .header(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, ADMIN_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_AUTHORIZATION_DENIED"));
    }

    private void makeManualReviewEvent() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/wallet-001/charges")
                        .with(jwt().jwt(token -> token.subject("member-001")))
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
