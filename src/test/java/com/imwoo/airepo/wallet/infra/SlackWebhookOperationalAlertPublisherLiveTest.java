package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.AiRepoApplication;
import com.imwoo.airepo.wallet.application.OperationalAlertPublisher;
import com.imwoo.airepo.wallet.domain.OperationalAlert;
import com.imwoo.airepo.wallet.domain.OperationalAlertSeverity;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = AiRepoApplication.class)
@TestPropertySource(properties = "ai-repo.operational-alert.publisher.type=slack-webhook")
@EnabledIfEnvironmentVariable(named = "AI_REPO_LIVE_SLACK_TEST", matches = "true")
class SlackWebhookOperationalAlertPublisherLiveTest {

    private final OperationalAlertPublisher operationalAlertPublisher;

    @Autowired
    SlackWebhookOperationalAlertPublisherLiveTest(OperationalAlertPublisher operationalAlertPublisher) {
        this.operationalAlertPublisher = operationalAlertPublisher;
    }

    @Test
    void publishesOperationalAlertToRealSlackWebhook() {
        operationalAlertPublisher.publish(new OperationalAlert(
                "operational-alert-live-test",
                "LIVE_TEST",
                OperationalAlertSeverity.WARNING,
                Instant.now(),
                List.of("live Slack webhook integration test")
        ));
    }
}
