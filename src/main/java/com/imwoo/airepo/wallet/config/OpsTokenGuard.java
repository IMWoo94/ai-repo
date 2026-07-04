package com.imwoo.airepo.wallet.config;

import com.imwoo.airepo.wallet.api.AdminAuthorizationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails application startup if a built-in development ops token is left in place under a deployed
 * profile ({@code postgres} or {@code prod}; see {@link DeployedProfiles}). The default admin and
 * operator tokens are published in the repository, so they grant anyone operator/admin access; under
 * a deployed profile they must be overridden via {@code AI_REPO_OPS_ADMIN_TOKEN} and
 * {@code AI_REPO_OPS_OPERATOR_TOKEN}. Other profiles only get a warning so local/test runs keep working.
 */
@Component
class OpsTokenGuard {

    private static final String INSECURE_DEFAULT_ADMIN_TOKEN = "local-ops-token";
    private static final String INSECURE_DEFAULT_OPERATOR_TOKEN = "local-operator-token";
    private static final Logger log = LoggerFactory.getLogger(OpsTokenGuard.class);

    OpsTokenGuard(AdminAuthorizationProperties properties, Environment environment) {
        boolean adminIsDefault = INSECURE_DEFAULT_ADMIN_TOKEN.equals(properties.adminToken());
        boolean operatorIsDefault = INSECURE_DEFAULT_OPERATOR_TOKEN.equals(properties.operatorToken());
        if (!adminIsDefault && !operatorIsDefault) {
            return;
        }
        if (DeployedProfiles.isActive(environment)) {
            if (adminIsDefault) {
                throw new IllegalStateException(
                        "ai-repo.ops.admin-token is the built-in development default; "
                                + "set AI_REPO_OPS_ADMIN_TOKEN for deployed profiles (postgres/prod)");
            }
            throw new IllegalStateException(
                    "ai-repo.ops.operator-token is the built-in development default; "
                            + "set AI_REPO_OPS_OPERATOR_TOKEN for deployed profiles (postgres/prod)");
        }
        if (adminIsDefault) {
            log.warn("ai-repo.ops.admin-token is the built-in development default — "
                    + "set AI_REPO_OPS_ADMIN_TOKEN before deploying.");
        }
        if (operatorIsDefault) {
            log.warn("ai-repo.ops.operator-token is the built-in development default — "
                    + "set AI_REPO_OPS_OPERATOR_TOKEN before deploying.");
        }
    }
}
