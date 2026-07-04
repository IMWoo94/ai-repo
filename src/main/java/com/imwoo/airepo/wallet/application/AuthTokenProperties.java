package com.imwoo.airepo.wallet.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthTokenProperties {

    private static final int MIN_SECRET_LENGTH = 32;

    private final String secret;
    private final long ttlMinutes;

    public AuthTokenProperties(
            @Value("${ai-repo.auth.jwt.secret:local-dev-jwt-secret-please-change-32b}") String secret,
            @Value("${ai-repo.auth.jwt.ttl-minutes:60}") long ttlMinutes
    ) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException("ai-repo.auth.jwt.secret must be at least 32 chars (HMAC-SHA256)");
        }
        if (ttlMinutes <= 0) {
            throw new IllegalArgumentException("ai-repo.auth.jwt.ttl-minutes must be positive");
        }
        this.secret = secret;
        this.ttlMinutes = ttlMinutes;
    }

    public String secret() {
        return secret;
    }

    public long ttlMinutes() {
        return ttlMinutes;
    }
}
