package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.application.InMemoryWalletCommandService;
import com.imwoo.airepo.wallet.application.OperationOutboxPublishBatchResult;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import com.imwoo.airepo.wallet.application.WalletChargeCommand;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class HttpOperationOutboxPublisherContractTest {

    @Test
    void publishesOutboxEventAsHttpJsonEnvelope() throws Exception {
        try (BrokerEndpoint brokerEndpoint = BrokerEndpoint.responding(202)) {
            HttpOperationOutboxPublisher publisher = new HttpOperationOutboxPublisher(
                    brokerEndpoint.endpoint(),
                    3000
            );

            publisher.publish(outboxEvent());

            assertThat(brokerEndpoint.method()).isEqualTo("POST");
            assertThat(brokerEndpoint.path()).isEqualTo("/outbox-events");
            assertThat(brokerEndpoint.header("Content-Type")).isEqualTo("application/json");
            assertThat(brokerEndpoint.header("X-Outbox-Event-Id")).isEqualTo("outbox-001");
            assertThat(brokerEndpoint.body())
                    .contains("\"outboxEventId\":\"outbox-001\"")
                    .contains("\"operationId\":\"op-001\"")
                    .contains("\"eventType\":\"CHARGE_COMPLETED\"")
                    .contains("\"aggregateType\":\"WALLET_OPERATION\"")
                    .contains("\"aggregateId\":\"op-001\"")
                    .contains("\"occurredAt\":\"2026-05-01T00:00:00Z\"")
                    .contains("\"payload\":{\"operationId\":\"op-001\"");
        }
    }

    @Test
    void failsWhenBrokerReturnsNon2xxStatus() throws Exception {
        try (BrokerEndpoint brokerEndpoint = BrokerEndpoint.responding(503)) {
            HttpOperationOutboxPublisher publisher = new HttpOperationOutboxPublisher(
                    brokerEndpoint.endpoint(),
                    3000
            );

            assertThatThrownBy(() -> publisher.publish(outboxEvent()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("broker returned non-2xx status: 503");
        }
    }

    @Test
    void relayMarksOutboxEventFailedWhenHttpBrokerFails() throws Exception {
        try (BrokerEndpoint brokerEndpoint = BrokerEndpoint.responding(503)) {
            InMemoryWalletRepository repository = new InMemoryWalletRepository();
            InMemoryWalletCommandService commandService = new InMemoryWalletCommandService(
                    Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
                    repository
            );
            commandService.charge("wallet-001", new WalletChargeCommand(
                    new Money(new BigDecimal("5000"), "KRW"),
                    "charge-http-001",
                    "HTTP broker failure"
            ));
            OperationOutboxRelayService relayService = new OperationOutboxRelayService(
                    Clock.fixed(Instant.parse("2026-05-01T00:01:00Z"), ZoneOffset.UTC),
                    repository,
                    new HttpOperationOutboxPublisher(brokerEndpoint.endpoint(), 3000)
            );

            OperationOutboxPublishBatchResult result = relayService.publishReadyEvents(10);

            assertThat(result.claimedCount()).isEqualTo(1);
            assertThat(result.publishedCount()).isZero();
            assertThat(result.failedCount()).isEqualTo(1);
            assertThat(repository.findOperationOutboxEvents("op-001"))
                    .singleElement()
                    .satisfies(outboxEvent -> {
                        assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.FAILED);
                        assertThat(outboxEvent.lastError()).isEqualTo("broker returned non-2xx status: 503");
                    });
        }
    }

    private OperationOutboxEvent outboxEvent() {
        return new OperationOutboxEvent(
                "outbox-001",
                "op-001",
                "CHARGE_COMPLETED",
                "WALLET_OPERATION",
                "op-001",
                "{\"operationId\":\"op-001\",\"walletId\":\"wallet-001\",\"counterpartyWalletId\":null,\"type\":\"CHARGE\",\"amount\":\"5000\",\"currency\":\"KRW\"}",
                OperationOutboxStatus.PROCESSING,
                Instant.parse("2026-05-01T00:00:00Z"),
                0,
                null,
                Instant.parse("2026-05-01T00:01:00Z"),
                Instant.parse("2026-05-01T00:02:00Z"),
                null,
                null
        );
    }

    private static class BrokerEndpoint implements AutoCloseable {

        private final HttpServer server;
        private final AtomicReference<CapturedRequest> capturedRequest = new AtomicReference<>();

        private BrokerEndpoint(HttpServer server) {
            this.server = server;
        }

        static BrokerEndpoint responding(int statusCode) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            BrokerEndpoint brokerEndpoint = new BrokerEndpoint(server);
            server.createContext("/outbox-events", exchange -> brokerEndpoint.handle(exchange, statusCode));
            server.start();
            return brokerEndpoint;
        }

        String endpoint() {
            return "http://127.0.0.1:%d/outbox-events".formatted(server.getAddress().getPort());
        }

        String method() {
            return capturedRequest.get().method();
        }

        String path() {
            return capturedRequest.get().path();
        }

        String header(String name) {
            return capturedRequest.get().header(name);
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
                    exchange.getRequestHeaders().getFirst("X-Outbox-Event-Id"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
            ));
            byte[] responseBody = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        }
    }

    private record CapturedRequest(
            String method,
            String path,
            String contentType,
            String outboxEventId,
            String body
    ) {

        String header(String name) {
            if ("Content-Type".equals(name)) {
                return contentType;
            }
            if ("X-Outbox-Event-Id".equals(name)) {
                return outboxEventId;
            }
            return null;
        }
    }
}
