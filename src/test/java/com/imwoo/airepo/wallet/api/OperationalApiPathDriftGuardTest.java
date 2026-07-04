package com.imwoo.airepo.wallet.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * 운영 API 경로 목록은 {@link SecurityConfig#OPERATIONAL_API_PATHS}(인증 chain의 securityMatcher)와
 * {@link AdminApiPathMatcher#ADMIN_API_PATH_PREFIXES}(헤더 인증·접근 감사 필터)에 별도 상수로 존재한다.
 * 한쪽만 갱신하면 drift(#109 계열)가 재발하므로, 두 목록이 같은 운영 API 경로 집합을 다루는지 검증한다.
 */
class OperationalApiPathDriftGuardTest {

    @Test
    void securityConfigPatternsAndMatcherPrefixesCoverSameRoots() throws Exception {
        Set<String> securityRoots = securityConfigOperationalRoots();
        Set<String> matcherPrefixes = adminApiPathMatcherPrefixes();

        Set<String> missingInMatcher = new TreeSet<>(securityRoots);
        missingInMatcher.removeAll(matcherPrefixes);
        assertThat(missingInMatcher)
                .as("SecurityConfig 운영 patterns에는 있으나 AdminApiPathMatcher prefix에 없는 경로: %s", missingInMatcher)
                .isEmpty();

        Set<String> missingInSecurityConfig = new TreeSet<>(matcherPrefixes);
        missingInSecurityConfig.removeAll(securityRoots);
        assertThat(missingInSecurityConfig)
                .as("AdminApiPathMatcher prefix에는 있으나 SecurityConfig 운영 patterns에 없는 경로: %s",
                        missingInSecurityConfig)
                .isEmpty();
    }

    @Test
    void matcherMatchesEverySecurityConfigOperationalRootAndSubPath() throws Exception {
        for (String root : securityConfigOperationalRoots()) {
            assertThat(AdminApiPathMatcher.isAdminApiPath(root))
                    .as("운영 API root는 matcher가 true여야 함: %s", root)
                    .isTrue();
            assertThat(AdminApiPathMatcher.isAdminApiPath(root + "/sub-resource"))
                    .as("운영 API 하위 path는 matcher가 true여야 함: %s/sub-resource", root)
                    .isTrue();
            assertThat(AdminApiPathMatcher.isAdminApiPath(root + "-lookalike"))
                    .as("운영 API prefix를 흉내 낸 lookalike path는 matcher가 false여야 함: %s-lookalike", root)
                    .isFalse();
        }
    }

    @Test
    void matcherRejectsCuratedLookalikePrefixes() {
        List<String> lookalikes = List.of(
                "/api/v1/outbox-events-v2",
                "/api/v1/outbox-consumerish",
                "/api/v1/operations-summary",
                "/api/v1/audit-events-export",
                "/api/v1/wallets/wallet-001/audit-events"
        );
        for (String lookalike : lookalikes) {
            assertThat(AdminApiPathMatcher.isAdminApiPath(lookalike))
                    .as("lookalike prefix는 운영 API로 분류되면 안 됨: %s", lookalike)
                    .isFalse();
        }
    }

    private static Set<String> securityConfigOperationalRoots() throws Exception {
        Field field = SecurityConfig.class.getDeclaredField("OPERATIONAL_API_PATHS");
        field.setAccessible(true);
        String[] patterns = (String[]) field.get(null);
        return java.util.Arrays.stream(patterns)
                .map(OperationalApiPathDriftGuardTest::stripSuffixWildcard)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> adminApiPathMatcherPrefixes() throws Exception {
        Field field = AdminApiPathMatcher.class.getDeclaredField("ADMIN_API_PATH_PREFIXES");
        field.setAccessible(true);
        List<String> prefixes = (List<String>) field.get(null);
        return new TreeSet<>(prefixes);
    }

    private static String stripSuffixWildcard(String pattern) {
        String normalized = pattern;
        if (normalized.endsWith("/**")) {
            normalized = normalized.substring(0, normalized.length() - "/**".length());
        }
        return normalized;
    }
}
