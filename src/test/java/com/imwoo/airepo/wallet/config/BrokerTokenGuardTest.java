package com.imwoo.airepo.wallet.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.api.BrokerAuthorizationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class BrokerTokenGuardTest {

    private static final String DEFAULT_TOKEN = "local-broker-token";
    private static final String OVERRIDDEN_TOKEN = "a-strong-injected-broker-token";

    @Test
    void failsFastWhenProdProfileUsesBuiltInDefaultToken() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new BrokerTokenGuard(new BrokerAuthorizationProperties(DEFAULT_TOKEN), environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_REPO_OUTBOX_CONSUMER_BROKER_TOKEN");
    }

    @Test
    void failsFastWhenPostgresProfileUsesBuiltInDefaultToken() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("postgres");

        assertThatThrownBy(() -> new BrokerTokenGuard(new BrokerAuthorizationProperties(DEFAULT_TOKEN), environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_REPO_OUTBOX_CONSUMER_BROKER_TOKEN");
    }

    @Test
    void allowsProdProfileWithOverriddenToken() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatCode(() -> new BrokerTokenGuard(new BrokerAuthorizationProperties(OVERRIDDEN_TOKEN), environment))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsPostgresProfileWithOverriddenToken() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("postgres");

        assertThatCode(() -> new BrokerTokenGuard(new BrokerAuthorizationProperties(OVERRIDDEN_TOKEN), environment))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsNonDeployedProfileWithBuiltInDefaultToken() {
        MockEnvironment environment = new MockEnvironment();

        assertThatCode(() -> new BrokerTokenGuard(new BrokerAuthorizationProperties(DEFAULT_TOKEN), environment))
                .doesNotThrowAnyException();
    }
}
