package com.imwoo.airepo.wallet.config;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails application startup if a deployed profile ({@code postgres} or {@code prod}; see
 * {@link DeployedProfiles}) runs without the {@code postgres} profile. JDBC persistence
 * ({@code JdbcWalletRepository}) is only active under {@code postgres}, while
 * {@code InMemoryWalletRepository} loads under {@code !postgres}. A {@code prod}-only startup would
 * therefore pass the credential guards yet load the in-memory store, silently losing balances on
 * restart. Requiring {@code postgres} under any deployed profile turns that data-loss trap into an
 * explicit startup failure. Non-deployed profiles (local, test, no profile) keep the in-memory
 * store for developer runs.
 */
@Component
class DeployedProfilePersistenceGuard {

    private static final Logger log = LoggerFactory.getLogger(DeployedProfilePersistenceGuard.class);

    DeployedProfilePersistenceGuard(Environment environment) {
        if (!DeployedProfiles.isActive(environment)) {
            return;
        }
        boolean postgresActive = Arrays.stream(environment.getActiveProfiles())
                .anyMatch("postgres"::equals);
        if (!postgresActive) {
            throw new IllegalStateException(
                    "deployed profile requires the 'postgres' profile for JDBC persistence; "
                            + "in-memory wallet store must not run in a deployed profile");
        }
        log.info("deployed profile with 'postgres' active — JDBC wallet persistence in use.");
    }
}
