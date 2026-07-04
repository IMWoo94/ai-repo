package com.imwoo.airepo.wallet.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AdminApiPathMatcherTest {

    @Test
    void rejectsNullPath() {
        assertThat(AdminApiPathMatcher.isAdminApiPath(null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/outbox-events",
            "/api/v1/outbox-events/outbox-001/requeue-audits",
            "/api/v1/outbox-consumer",
            "/api/v1/outbox-consumer/metrics",
            "/api/v1/outbox-relay-runs",
            "/api/v1/outbox-relay-runs/health",
            "/api/v1/admin-api-access-audits",
            "/api/v1/operational-alerts",
            "/api/v1/operational-log-pruning-runs",
            "/api/v1/audit-events",
            "/api/v1/operations",
            "/api/v1/operations/op-001/step-logs",
            "/api/v1/operations/op-001/outbox-events",
            "/api/v1/test-fixtures",
            "/api/v1/test-fixtures/outbox-events/manual-review"
    })
    void matchesAdminApiRootAndSubPaths(String requestUri) {
        assertThat(AdminApiPathMatcher.isAdminApiPath(requestUri)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "/api/v1/outbox-events-v2",
            "/api/v1/outbox-consumerish",
            "/api/v1/outbox-relay-runs-health",
            "/api/v1/admin-api-access-audits-export",
            "/api/v1/operational-alerts-public",
            "/api/v1/operational-log-pruning-runs-preview",
            "/api/v1/audit-events-export",
            "/api/v1/operations-summary",
            "/api/v1/wallets/wallet-001/audit-events",
            "/api/v1/wallets/wallet-001/balance"
    })
    void rejectsLookalikePrefixesAndPublicPaths(String requestUri) {
        assertThat(AdminApiPathMatcher.isAdminApiPath(requestUri)).isFalse();
    }
}
