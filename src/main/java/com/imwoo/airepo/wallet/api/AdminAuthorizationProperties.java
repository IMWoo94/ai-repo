package com.imwoo.airepo.wallet.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthorizationProperties {

    private final String adminToken;
    private final String operatorToken;

    public AdminAuthorizationProperties(
            @Value("${ai-repo.ops.admin-token:local-ops-token}") String adminToken,
            @Value("${ai-repo.ops.operator-token:local-operator-token}") String operatorToken
    ) {
        if (adminToken == null || adminToken.isBlank()) {
            throw new IllegalArgumentException("ai-repo.ops.admin-token must not be blank");
        }
        if (operatorToken == null || operatorToken.isBlank()) {
            throw new IllegalArgumentException("ai-repo.ops.operator-token must not be blank");
        }
        this.adminToken = adminToken;
        this.operatorToken = operatorToken;
    }

    public String adminToken() {
        return adminToken;
    }

    public String operatorToken() {
        return operatorToken;
    }
}
