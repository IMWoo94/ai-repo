package com.imwoo.airepo.wallet.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> ADMIN_API_PATH_PREFIXES = List.of(
            "/api/v1/outbox-events",
            "/api/v1/outbox-relay-runs",
            "/api/v1/admin-api-access-audits",
            "/api/v1/operational-log-pruning-runs"
    );

    private final AdminAuthorizationProperties properties;
    private final AdminSecurityErrorHandler adminSecurityErrorHandler;

    public AdminHeaderAuthenticationFilter(
            AdminAuthorizationProperties properties,
            AdminSecurityErrorHandler adminSecurityErrorHandler
    ) {
        this.properties = properties;
        this.adminSecurityErrorHandler = adminSecurityErrorHandler;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isAdminApiPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        String adminToken = request.getHeader(AdminAuthorizationGuard.ADMIN_TOKEN_HEADER);
        String operatorToken = request.getHeader(AdminAuthorizationGuard.OPERATOR_TOKEN_HEADER);
        boolean adminAuthenticated = tokenMatches(properties.adminToken(), adminToken);
        boolean operatorAuthenticated = tokenMatches(properties.operatorToken(), operatorToken);
        if (!adminAuthenticated && !operatorAuthenticated) {
            adminSecurityErrorHandler.commence(
                    request,
                    response,
                    new BadCredentialsException("operator or admin token is required")
            );
            return;
        }

        String operatorId = request.getHeader(AdminAuthorizationGuard.OPERATOR_ID_HEADER);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal(operatorId),
                null,
                authorities(operatorId, adminAuthenticated)
        );
        try {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isAdminApiPath(String requestUri) {
        return ADMIN_API_PATH_PREFIXES.stream().anyMatch(requestUri::startsWith);
    }

    private String principal(String operatorId) {
        if (operatorId == null || operatorId.isBlank()) {
            return "missing-operator";
        }
        return operatorId.trim();
    }

    private List<SimpleGrantedAuthority> authorities(String operatorId, boolean adminAuthenticated) {
        if (operatorId == null || operatorId.isBlank()) {
            return List.of();
        }
        if (adminAuthenticated) {
            return List.of(
                    authority(AdminSecurityRole.OPERATOR),
                    authority(AdminSecurityRole.ADMIN)
            );
        }
        return List.of(authority(AdminSecurityRole.OPERATOR));
    }

    private SimpleGrantedAuthority authority(AdminSecurityRole role) {
        return new SimpleGrantedAuthority("ROLE_" + role.name());
    }

    private boolean tokenMatches(String expectedToken, String actualToken) {
        if (actualToken == null || actualToken.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                actualToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
