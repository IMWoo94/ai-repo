package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.AuthToken;
import java.time.Instant;

public record AuthTokenResponse(String token, String memberId, Instant expiresAt) {

    static AuthTokenResponse from(AuthToken token) {
        return new AuthTokenResponse(token.token(), token.memberId(), token.expiresAt());
    }
}
