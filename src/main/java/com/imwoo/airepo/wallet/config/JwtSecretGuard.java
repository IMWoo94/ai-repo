package com.imwoo.airepo.wallet.config;

import com.imwoo.airepo.wallet.application.AuthTokenProperties;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails application startup if the built-in development JWT secret is left in place under the
 * {@code prod} profile. The default secret is published in the repository, so a token signed with
 * it can be forged for any member; under a production profile it must be overridden via
 * {@code AI_REPO_AUTH_JWT_SECRET}. Other profiles only get a warning so local/test runs keep working.
 */
@Component
class JwtSecretGuard {

    private static final String INSECURE_DEFAULT_SECRET = "local-dev-jwt-secret-please-change-32b";
    private static final Logger log = LoggerFactory.getLogger(JwtSecretGuard.class);

    JwtSecretGuard(AuthTokenProperties properties, Environment environment) {
        if (!INSECURE_DEFAULT_SECRET.equals(properties.secret())) {
            return;
        }
        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            throw new IllegalStateException(
                    "ai-repo.auth.jwt.secret is the built-in development default; "
                            + "set AI_REPO_AUTH_JWT_SECRET for the prod profile");
        }
        log.warn("ai-repo.auth.jwt.secret is the built-in development default — "
                + "set AI_REPO_AUTH_JWT_SECRET before deploying.");
    }
}
