package com.imwoo.airepo.wallet.api;

import java.util.List;

final class AdminApiPathMatcher {

    private static final List<String> ADMIN_API_PATH_PREFIXES = List.of(
            "/api/v1/outbox-events",
            "/api/v1/outbox-consumer",
            "/api/v1/outbox-relay-runs",
            "/api/v1/admin-api-access-audits",
            "/api/v1/operational-alerts",
            "/api/v1/operational-log-pruning-runs",
            "/api/v1/audit-events",
            "/api/v1/operations",
            "/api/v1/test-fixtures"
    );

    private AdminApiPathMatcher() {
    }

    static boolean isAdminApiPath(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return false;
        }
        return ADMIN_API_PATH_PREFIXES.stream().anyMatch(prefix -> matchesSegment(requestUri, prefix));
    }

    private static boolean matchesSegment(String requestUri, String prefix) {
        return requestUri.equals(prefix) || requestUri.startsWith(prefix + "/");
    }
}
