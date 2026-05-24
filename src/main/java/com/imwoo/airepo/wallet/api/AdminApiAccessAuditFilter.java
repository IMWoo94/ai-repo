package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.AdminApiAccessAuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminApiAccessAuditFilter extends OncePerRequestFilter {

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
        if (!AdminApiPathMatcher.isAdminApiPath(request.getRequestURI())) {
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
}
