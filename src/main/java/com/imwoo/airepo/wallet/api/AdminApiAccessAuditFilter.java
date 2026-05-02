package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.AdminApiAccessAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AdminApiAccessAuditFilter extends OncePerRequestFilter {

    private static final List<String> ADMIN_API_PATH_PREFIXES = List.of(
            "/api/v1/outbox-events",
            "/api/v1/outbox-relay-runs",
            "/api/v1/admin-api-access-audits"
    );

    private final AdminApiAccessAuditService adminApiAccessAuditService;

    public AdminApiAccessAuditFilter(AdminApiAccessAuditService adminApiAccessAuditService) {
        this.adminApiAccessAuditService = adminApiAccessAuditService;
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
        try {
            filterChain.doFilter(request, response);
        } finally {
            adminApiAccessAuditService.recordAccess(
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getHeader(AdminAuthorizationGuard.OPERATOR_ID_HEADER),
                    response.getStatus()
            );
        }
    }

    private boolean isAdminApiPath(String requestUri) {
        return ADMIN_API_PATH_PREFIXES.stream().anyMatch(requestUri::startsWith);
    }
}
