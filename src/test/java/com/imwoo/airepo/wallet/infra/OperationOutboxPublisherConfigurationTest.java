package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.imwoo.airepo.wallet.application.OperationOutboxPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class OperationOutboxPublisherConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    InMemoryOperationOutboxPublisher.class,
                    HttpOperationOutboxPublisher.class
            );

    @Test
    void usesInMemoryPublisherByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OperationOutboxPublisher.class);
            assertThat(context.getBean(OperationOutboxPublisher.class))
                    .isInstanceOf(InMemoryOperationOutboxPublisher.class);
        });
    }

    @Test
    void usesHttpPublisherWhenConfigured() {
        contextRunner
                .withPropertyValues(
                        "ai-repo.outbox.publisher.type=http",
                        "ai-repo.outbox.publisher.http.endpoint=http://127.0.0.1:18080/outbox-events"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OperationOutboxPublisher.class);
                    assertThat(context.getBean(OperationOutboxPublisher.class))
                            .isInstanceOf(HttpOperationOutboxPublisher.class);
                });
    }
}
