package com.imwoo.airepo.wallet.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class DeployedProfilePersistenceGuardTest {

    @Test
    void failsFastWhenProdProfileRunsWithoutPostgres() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new DeployedProfilePersistenceGuard(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("postgres");
    }

    @Test
    void allowsPostgresProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("postgres");

        assertThatCode(() -> new DeployedProfilePersistenceGuard(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsProdAndPostgresProfilesTogether() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod", "postgres");

        assertThatCode(() -> new DeployedProfilePersistenceGuard(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsNonDeployedProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        assertThatCode(() -> new DeployedProfilePersistenceGuard(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsNoActiveProfile() {
        MockEnvironment environment = new MockEnvironment();

        assertThatCode(() -> new DeployedProfilePersistenceGuard(environment))
                .doesNotThrowAnyException();
    }
}
