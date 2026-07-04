package com.imwoo.airepo.wallet.api;

import org.springframework.security.oauth2.jwt.Jwt;

final class WalletPrincipal {

    private WalletPrincipal() {
    }

    static String memberId(Jwt jwt) {
        return jwt.getSubject();
    }
}
