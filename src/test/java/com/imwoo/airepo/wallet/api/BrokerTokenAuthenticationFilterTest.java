package com.imwoo.airepo.wallet.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class BrokerTokenAuthenticationFilterTest {

    private final BrokerTokenAuthenticationFilter filter = new BrokerTokenAuthenticationFilter(
            new BrokerAuthorizationProperties("local-broker-token"),
            new BrokerSecurityErrorHandler(Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC))
    );

    @Test
    void passesThroughNonBrokerPathsWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/outbox-relay-runs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainCalled.set(true));

        assertThat(chainCalled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsBrokerRequestWithoutToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/broker/outbox-events");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainCalled.set(true));

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("BROKER_AUTHENTICATION_REQUIRED");
    }

    @Test
    void rejectsBrokerRequestWithWrongToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/broker/outbox-events");
        request.addHeader(BrokerTokenAuthenticationFilter.BROKER_TOKEN_HEADER, "wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainCalled.set(true));

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void passesBrokerRequestWithCorrectToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/broker/outbox-events");
        request.addHeader(BrokerTokenAuthenticationFilter.BROKER_TOKEN_HEADER, "local-broker-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> chainCalled.set(true));

        assertThat(chainCalled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
