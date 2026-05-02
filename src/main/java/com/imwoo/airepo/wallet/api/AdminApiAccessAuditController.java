package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.AdminApiAccessAuditService;
import com.imwoo.airepo.wallet.domain.AdminApiAccessAudit;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin-api-access-audits")
public class AdminApiAccessAuditController {

    private final AdminApiAccessAuditService adminApiAccessAuditService;
    private final AdminAuthorizationGuard adminAuthorizationGuard;

    public AdminApiAccessAuditController(
            AdminApiAccessAuditService adminApiAccessAuditService,
            AdminAuthorizationGuard adminAuthorizationGuard
    ) {
        this.adminApiAccessAuditService = adminApiAccessAuditService;
        this.adminAuthorizationGuard = adminAuthorizationGuard;
    }

    @GetMapping
    public List<AdminApiAccessAudit> recentAccessAudits(
            @RequestHeader(name = AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, required = false) String adminToken,
            @RequestHeader(name = AdminAuthorizationGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        adminAuthorizationGuard.authenticate(adminToken, operatorId);
        return adminApiAccessAuditService.getRecentAudits(limit);
    }
}
