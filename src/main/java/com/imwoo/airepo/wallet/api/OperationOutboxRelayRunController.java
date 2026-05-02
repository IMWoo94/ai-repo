package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.OperationOutboxRelayMonitoringService;
import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRun;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outbox-relay-runs")
public class OperationOutboxRelayRunController {

    private final OperationOutboxRelayMonitoringService operationOutboxRelayMonitoringService;
    private final AdminAuthorizationGuard adminAuthorizationGuard;

    public OperationOutboxRelayRunController(
            OperationOutboxRelayMonitoringService operationOutboxRelayMonitoringService,
            AdminAuthorizationGuard adminAuthorizationGuard
    ) {
        this.operationOutboxRelayMonitoringService = operationOutboxRelayMonitoringService;
        this.adminAuthorizationGuard = adminAuthorizationGuard;
    }

    @GetMapping
    public List<OperationOutboxRelayRun> recentRuns(
            @RequestHeader(name = AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, required = false) String adminToken,
            @RequestHeader(name = AdminAuthorizationGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        adminAuthorizationGuard.authenticate(adminToken, operatorId);
        return operationOutboxRelayMonitoringService.getRecentRuns(limit);
    }
}
