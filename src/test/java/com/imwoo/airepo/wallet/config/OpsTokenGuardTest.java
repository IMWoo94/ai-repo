package com.imwoo.airepo.wallet.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.api.AdminAuthorizationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class OpsTokenGuardTest {

    private static final String DEFAULT_ADMIN = "local-ops-token";
    private static final String DEFAULT_OPERATOR = "local-operator-token";
    private static final String OVERRIDDEN = "a-real-ops-token";

    @Test
    void failsFastWhenProdProfileUsesBuiltInDefaultAdminToken() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new OpsTokenGuard(
                new AdminAuthorizationProperties(DEFAULT_ADMIN, OVERRIDDEN), environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_REPO_OPS_ADMIN_TOKEN");
    }

    @Test
    void failsFastWhenPostgresProfileUsesBuiltInDefaultAdminToken() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("postgres");

        assertThatThrownBy(() -> new OpsTokenGuard(
                new AdminAuthorizationProperties(DEFAULT_ADMIN, OVERRIDDEN), environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_REPO_OPS_ADMIN_TOKEN");
    }

    @Test
    void failsFastWhenPostgresProfileUsesBuiltInDefaultOperatorToken() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("postgres");

        assertThatThrownBy(() -> new OpsTokenGuard(
                new AdminAuthorizationProperties(OVERRIDDEN, DEFAULT_OPERATOR), environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AI_REPO_OPS_OPERATOR_TOKEN");
    }

    @Test
    void allowsPostgresProfileWithOverriddenTokens() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("postgres");

        assertThatCode(() -> new OpsTokenGuard(
                new AdminAuthorizationProperties(OVERRIDDEN, OVERRIDDEN), environment))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsNonDeployedProfileWithBuiltInDefaultTokens() {
        MockEnvironment environment = new MockEnvironment();

        assertThatCode(() -> new OpsTokenGuard(
                new AdminAuthorizationProperties(DEFAULT_ADMIN, DEFAULT_OPERATOR), environment))
                .doesNotThrowAnyException();
    }
}
