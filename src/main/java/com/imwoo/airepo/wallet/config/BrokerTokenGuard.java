package com.imwoo.airepo.wallet.config;

import com.imwoo.airepo.wallet.api.BrokerAuthorizationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails application startup if the built-in development broker token is left in place under a
 * deployed profile ({@code postgres} or {@code prod}; see {@link DeployedProfiles}). The default
 * token is published in the repository, so anyone who can reach {@code /internal/broker/**} could
 * forge an authenticated outbox event; under a deployed profile it must be overridden via
 * {@code AI_REPO_OUTBOX_CONSUMER_BROKER_TOKEN}. Other profiles only get a warning so local/test
 * runs keep working. Mirrors {@link JwtSecretGuard}.
 */
@Component
class BrokerTokenGuard {

    private static final String INSECURE_DEFAULT_TOKEN = "local-broker-token";
    private static final Logger log = LoggerFactory.getLogger(BrokerTokenGuard.class);

    BrokerTokenGuard(BrokerAuthorizationProperties properties, Environment environment) {
        if (!INSECURE_DEFAULT_TOKEN.equals(properties.brokerToken())) {
            return;
        }
        if (DeployedProfiles.isActive(environment)) {
            throw new IllegalStateException(
                    "ai-repo.outbox.consumer.broker-token is the built-in development default; "
                            + "set AI_REPO_OUTBOX_CONSUMER_BROKER_TOKEN for deployed profiles (postgres/prod)");
        }
        log.warn("ai-repo.outbox.consumer.broker-token is the built-in development default — "
                + "set AI_REPO_OUTBOX_CONSUMER_BROKER_TOKEN before deploying.");
    }
}
