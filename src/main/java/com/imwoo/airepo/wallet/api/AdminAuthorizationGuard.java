package com.imwoo.airepo.wallet.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthorizationGuard {

    public static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    public static final String OPERATOR_TOKEN_HEADER = "X-Operator-Token";
    public static final String OPERATOR_ID_HEADER = "X-Operator-Id";

    private final AdminAuthorizationProperties properties;

    public AdminAuthorizationGuard(AdminAuthorizationProperties properties) {
        this.properties = properties;
    }

    public AdminOperator authenticate(String adminToken, String operatorId) {
        if (adminToken == null || adminToken.isBlank()) {
            throw new AdminAuthenticationException("admin token is required");
        }
        if (!tokensEqual(properties.adminToken(), adminToken)) {
            throw new AdminAuthenticationException("admin token is invalid");
        }
        if (operatorId == null || operatorId.isBlank()) {
            throw new AdminAuthorizationException("operator id is required");
        }
        return new AdminOperator(operatorId.trim());
    }

    private boolean tokensEqual(String expectedToken, String actualToken) {
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                actualToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
