package com.imwoo.airepo.wallet.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthorizationProperties {

    private final String adminToken;

    public AdminAuthorizationProperties(@Value("${ai-repo.ops.admin-token:local-ops-token}") String adminToken) {
        if (adminToken == null || adminToken.isBlank()) {
            throw new IllegalArgumentException("ai-repo.ops.admin-token must not be blank");
        }
        this.adminToken = adminToken;
    }

    public String adminToken() {
        return adminToken;
    }
}
