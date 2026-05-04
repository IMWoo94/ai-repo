package com.imwoo.airepo.wallet.api;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class AdminSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final Clock clock;

    public AdminSecurityErrorHandler(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        writeError(
                response,
                HttpStatus.UNAUTHORIZED,
                "ADMIN_AUTHENTICATION_REQUIRED",
                message(authException, "admin token is required")
        );
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        writeError(
                response,
                HttpStatus.FORBIDDEN,
                "ADMIN_AUTHORIZATION_DENIED",
                "operator id is required"
        );
    }

    private void writeError(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("""
                {"code":"%s","message":"%s","timestamp":"%s"}"""
                .formatted(escape(code), escape(message), Instant.now(clock)));
    }

    private String message(AuthenticationException exception, String fallbackMessage) {
        if (exception.getMessage() == null || exception.getMessage().isBlank()) {
            return fallbackMessage;
        }
        return exception.getMessage();
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
