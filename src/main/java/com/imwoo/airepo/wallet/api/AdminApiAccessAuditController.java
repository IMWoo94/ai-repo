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

    public AdminApiAccessAuditController(AdminApiAccessAuditService adminApiAccessAuditService) {
        this.adminApiAccessAuditService = adminApiAccessAuditService;
    }

    @GetMapping
    public List<AdminApiAccessAudit> recentAccessAudits(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return adminApiAccessAuditService.getRecentAudits(limit);
    }
}
