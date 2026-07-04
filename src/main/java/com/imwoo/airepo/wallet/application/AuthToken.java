package com.imwoo.airepo.wallet.application;

import java.time.Instant;
import java.util.Objects;

public record AuthToken(String token, String memberId, Instant expiresAt) {

    public AuthToken {
        Objects.requireNonNull(token, "token must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }
}
