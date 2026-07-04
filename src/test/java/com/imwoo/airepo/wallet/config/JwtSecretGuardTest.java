package com.imwoo.airepo.wallet.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.application.AuthTokenProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtSecretGuardTest {

    private static final String DEFAULT_SECRET = "local-dev-jwt-secret-please-change-32b";
    private static final String OVERRIDDEN_SECRET = "a-32-char-or-longer-production-secret!!";

    @Test
    void failsFastWhenProdProfileUsesBuiltInDefaultSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new JwtSecretGuard(new AuthTokenProperties(DEFAULT_SECRET, 60), environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_REPO_AUTH_JWT_SECRET");
    }

    @Test
    void failsFastWhenPostgresProfileUsesBuiltInDefaultSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("postgres");

        assertThatThrownBy(() -> new JwtSecretGuard(new AuthTokenProperties(DEFAULT_SECRET, 60), environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_REPO_AUTH_JWT_SECRET");
    }

    @Test
    void allowsProdProfileWithOverriddenSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatCode(() -> new JwtSecretGuard(new AuthTokenProperties(OVERRIDDEN_SECRET, 60), environment))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsPostgresProfileWithOverriddenSecret() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("postgres");

        assertThatCode(() -> new JwtSecretGuard(new AuthTokenProperties(OVERRIDDEN_SECRET, 60), environment))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsNonDeployedProfileWithBuiltInDefaultSecret() {
        MockEnvironment environment = new MockEnvironment();

        assertThatCode(() -> new JwtSecretGuard(new AuthTokenProperties(DEFAULT_SECRET, 60), environment))
                .doesNotThrowAnyException();
    }
}
