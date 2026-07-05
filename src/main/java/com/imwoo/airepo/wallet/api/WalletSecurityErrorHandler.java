package com.imwoo.airepo.wallet.api;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class WalletSecurityErrorHandler implements AuthenticationEntryPoint {

    private final Clock clock;

    public WalletSecurityErrorHandler(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"code":"%s","message":"%s","timestamp":"%s"}"""
                .formatted("WALLET_AUTHENTICATION_REQUIRED", message(authException), Instant.now(clock)));
    }

    private String message(AuthenticationException exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "wallet authentication is required";
        }
        return exception.getMessage();
    }
}
