package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.AuthTokenService;
import com.imwoo.airepo.wallet.application.InvalidWalletOperationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthTokenController {

    private final AuthTokenService authTokenService;

    public AuthTokenController(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @PostMapping("/tokens")
    public AuthTokenResponse issueToken(@RequestBody AuthTokenRequest request) {
        if (request.memberId() == null || request.memberId().isBlank()) {
            throw new InvalidWalletOperationException("memberId must not be blank");
        }
        return AuthTokenResponse.from(authTokenService.issueToken(request.memberId()));
    }
}
