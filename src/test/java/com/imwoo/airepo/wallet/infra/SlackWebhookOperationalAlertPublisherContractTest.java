package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.OperationalAlert;
import com.imwoo.airepo.wallet.domain.OperationalAlertSeverity;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SlackWebhookOperationalAlertPublisherContractTest {

    @Test
    void publishesOperationalAlertAsSlackWebhookTextPayload() throws Exception {
        try (SlackEndpoint slackEndpoint = SlackEndpoint.responding(200)) {
            SlackWebhookOperationalAlertPublisher publisher = new SlackWebhookOperationalAlertPublisher(
                    slackEndpoint.endpoint(),
                    3000
            );

            publisher.publish(operationalAlert());

            assertThat(slackEndpoint.method()).isEqualTo("POST");
            assertThat(slackEndpoint.path()).isEqualTo("/slack/webhook");
            assertThat(slackEndpoint.contentType()).isEqualTo("application/json");
            assertThat(slackEndpoint.body())
                    .contains("\"text\":\"[CRITICAL] OUTBOX_CONSUMER operational alert at 2026-05-01T00:05:00Z")
                    .contains("alertId: operational-alert-001")
                    .contains("- critical consumer duplicate delivery rate in health window")
                    .contains("\\n- recent duplicate count exceeded threshold");
        }
    }

    @Test
    void failsWhenSlackWebhookReturnsNon2xxStatus() throws Exception {
        try (SlackEndpoint slackEndpoint = SlackEndpoint.responding(503)) {
            SlackWebhookOperationalAlertPublisher publisher = new SlackWebhookOperationalAlertPublisher(
                    slackEndpoint.endpoint(),
                    3000
            );

            assertThatThrownBy(() -> publisher.publish(operationalAlert()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("slack webhook returned non-2xx status: 503");
        }
    }

    private OperationalAlert operationalAlert() {
        return new OperationalAlert(
                "operational-alert-001",
                "OUTBOX_CONSUMER",
                OperationalAlertSeverity.CRITICAL,
                Instant.parse("2026-05-01T00:05:00Z"),
                List.of(
                        "critical consumer duplicate delivery rate in health window",
                        "recent duplicate count exceeded threshold"
                )
        );
    }

    private static class SlackEndpoint implements AutoCloseable {

        private final HttpServer server;
        private final AtomicReference<CapturedRequest> capturedRequest = new AtomicReference<>();

        private SlackEndpoint(HttpServer server) {
            this.server = server;
        }

        static SlackEndpoint responding(int statusCode) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            SlackEndpoint slackEndpoint = new SlackEndpoint(server);
            server.createContext("/slack/webhook", exchange -> slackEndpoint.handle(exchange, statusCode));
            server.start();
            return slackEndpoint;
        }

        String endpoint() {
            return "http://127.0.0.1:%d/slack/webhook".formatted(server.getAddress().getPort());
        }

        String method() {
            return capturedRequest.get().method();
        }

        String path() {
            return capturedRequest.get().path();
        }

        String contentType() {
            return capturedRequest.get().contentType();
        }

        String body() {
            return capturedRequest.get().body();
        }

        @Override
        public void close() {
            server.stop(0);
        }

        private void handle(HttpExchange exchange, int statusCode) throws IOException {
            capturedRequest.set(new CapturedRequest(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
            ));
            byte[] responseBody = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        }
    }

    private record CapturedRequest(
            String method,
            String path,
            String contentType,
            String body
    ) {
    }
}
