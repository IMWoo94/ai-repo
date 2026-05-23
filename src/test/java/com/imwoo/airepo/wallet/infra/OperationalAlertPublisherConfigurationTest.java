package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.imwoo.airepo.wallet.application.OperationalAlertPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OperationalAlertPublisherConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    NoopOperationalAlertPublisher.class,
                    SlackWebhookOperationalAlertPublisher.class
            );

    @Test
    void usesNoopPublisherByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OperationalAlertPublisher.class);
            assertThat(context.getBean(OperationalAlertPublisher.class))
                    .isInstanceOf(NoopOperationalAlertPublisher.class);
        });
    }

    @Test
    void usesSlackWebhookPublisherWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "ai-repo.operational-alert.publisher.type=slack-webhook",
                        "ai-repo.operational-alert.publisher.slack.webhook-url=http://127.0.0.1:18080/slack/webhook"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OperationalAlertPublisher.class);
                    assertThat(context.getBean(OperationalAlertPublisher.class))
                            .isInstanceOf(SlackWebhookOperationalAlertPublisher.class);
                });
    }
}
