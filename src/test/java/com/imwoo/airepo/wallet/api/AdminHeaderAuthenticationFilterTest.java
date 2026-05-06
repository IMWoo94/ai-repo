package com.imwoo.airepo.wallet.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminHeaderAuthenticationFilterTest {

    private final AdminHeaderAuthenticationFilter filter = new AdminHeaderAuthenticationFilter(
            new AdminAuthorizationProperties("local-ops-token", "local-operator-token"),
            new AdminSecurityErrorHandler(Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC))
    );

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesAdminHeadersWithOperatorAndAdminRoles() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/outbox-relay-runs");
        request.addHeader(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, "local-ops-token");
        request.addHeader(AdminAuthorizationGuard.OPERATOR_ID_HEADER, " ops-user ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            authentication.set(SecurityContextHolder.getContext().getAuthentication());
        });

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(authentication.get().getName()).isEqualTo("ops-user");
        assertThat(authentication.get().getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_OPERATOR", "ROLE_ADMIN");
    }

    @Test
    void authenticatesOperatorTokenWithOperatorRoleOnly() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/outbox-relay-runs");
        request.addHeader(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER, "local-operator-token");
        request.addHeader(AdminAuthorizationGuard.OPERATOR_ID_HEADER, " ops-user ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            authentication.set(SecurityContextHolder.getContext().getAuthentication());
        });

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(authentication.get().getName()).isEqualTo("ops-user");
        assertThat(authentication.get().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_OPERATOR");
    }

    @Test
    void validTokenWithoutOperatorHasNoRole() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/outbox-relay-runs");
        request.addHeader(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, "local-ops-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            authentication.set(SecurityContextHolder.getContext().getAuthentication());
        });

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(authentication.get().getName()).isEqualTo("missing-operator");
        assertThat(authentication.get().getAuthorities()).isEmpty();
    }

    @Test
    void rejectsMissingAdminToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/outbox-relay-runs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("ADMIN_AUTHENTICATION_REQUIRED");
        assertThat(response.getContentAsString()).contains("operator or admin token is required");
    }
}
