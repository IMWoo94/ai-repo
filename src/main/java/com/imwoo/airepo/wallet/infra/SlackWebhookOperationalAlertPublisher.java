package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationalAlertPublisher;
import com.imwoo.airepo.wallet.domain.OperationalAlert;
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
        prefix = "ai-repo.operational-alert.publisher",
        name = "type",
        havingValue = "slack-webhook"
)
public class SlackWebhookOperationalAlertPublisher implements OperationalAlertPublisher {

    private final HttpClient httpClient;
    private final URI webhookUrl;
    private final Duration timeout;

    public SlackWebhookOperationalAlertPublisher(
            @Value("${ai-repo.operational-alert.publisher.slack.webhook-url}") String webhookUrl,
            @Value("${ai-repo.operational-alert.publisher.slack.timeout-ms:3000}") long timeoutMillis
    ) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "ai-repo.operational-alert.publisher.slack.webhook-url must not be blank"
            );
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException(
                    "ai-repo.operational-alert.publisher.slack.timeout-ms must be positive"
            );
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMillis))
                .build();
        this.webhookUrl = URI.create(webhookUrl);
        this.timeout = Duration.ofMillis(timeoutMillis);
    }

    @Override
    public void publish(OperationalAlert operationalAlert) {
        HttpRequest request = HttpRequest.newBuilder(webhookUrl)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload(operationalAlert)))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("slack webhook returned non-2xx status: " + response.statusCode());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("slack webhook publish failed: " + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("slack webhook publish interrupted", exception);
        }
    }

    private String payload(OperationalAlert operationalAlert) {
        return """
                {"text":"%s"}"""
                .formatted(escape(text(operationalAlert)));
    }

    private String text(OperationalAlert operationalAlert) {
        return """
                [%s] %s operational alert at %s
                alertId: %s
                reasons:
                %s"""
                .formatted(
                        operationalAlert.severity(),
                        operationalAlert.source(),
                        operationalAlert.occurredAt(),
                        operationalAlert.alertId(),
                        "- " + String.join("\n- ", operationalAlert.reasons())
                );
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
