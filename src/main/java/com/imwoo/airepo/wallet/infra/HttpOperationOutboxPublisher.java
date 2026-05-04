package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationOutboxPublisher;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "ai-repo.outbox.publisher",
        name = "type",
        havingValue = "http"
)
public class HttpOperationOutboxPublisher implements OperationOutboxPublisher {

    private final HttpClient httpClient;
    private final URI endpoint;
    private final Duration timeout;

    public HttpOperationOutboxPublisher(
            @Value("${ai-repo.outbox.publisher.http.endpoint}") String endpoint,
            @Value("${ai-repo.outbox.publisher.http.timeout-ms:3000}") long timeoutMillis
    ) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("ai-repo.outbox.publisher.http.endpoint must not be blank");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("ai-repo.outbox.publisher.http.timeout-ms must be positive");
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMillis))
                .build();
        this.endpoint = URI.create(endpoint);
        this.timeout = Duration.ofMillis(timeoutMillis);
    }

    @Override
    public void publish(OperationOutboxEvent outboxEvent) {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("X-Outbox-Event-Id", outboxEvent.outboxEventId())
                .POST(HttpRequest.BodyPublishers.ofString(envelope(outboxEvent)))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("broker returned non-2xx status: " + response.statusCode());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("broker publish failed: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("broker publish interrupted", exception);
        }
    }

    private String envelope(OperationOutboxEvent outboxEvent) {
        return """
                {"outboxEventId":"%s","operationId":"%s","eventType":"%s","aggregateType":"%s","aggregateId":"%s","occurredAt":"%s","payload":%s}"""
                .formatted(
                        escape(outboxEvent.outboxEventId()),
                        escape(outboxEvent.operationId()),
                        escape(outboxEvent.eventType()),
                        escape(outboxEvent.aggregateType()),
                        escape(outboxEvent.aggregateId()),
                        outboxEvent.occurredAt(),
                        outboxEvent.payload()
                );
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
