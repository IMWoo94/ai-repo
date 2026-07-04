package com.imwoo.airepo.wallet.config;

import java.util.Arrays;
import org.springframework.core.env.Environment;

/**
 * Identifies profiles that represent a deployed (non local-dev/test) runtime. The real k8s
 * deployment runs the {@code postgres} profile (see {@code deploy/k8s/app.yaml}), so built-in
 * default credentials must never survive under {@code postgres} or {@code prod}. Other profiles
 * (local, test, no profile) are treated as developer environments where defaults are tolerated.
 */
final class DeployedProfiles {

    private DeployedProfiles() {
    }

    static boolean isActive(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equals(profile) || "postgres".equals(profile));
    }
}
