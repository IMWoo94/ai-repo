package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.OperationalLogPruningPolicy;
import com.imwoo.airepo.wallet.application.OperationalLogPruningResult;
import com.imwoo.airepo.wallet.application.OperationalLogPruningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operational-log-pruning-runs")
public class OperationalLogPruningController {

    private final OperationalLogPruningService operationalLogPruningService;
    private final OperationalLogPruningPolicy operationalLogPruningPolicy;
    private final AdminAuthorizationGuard adminAuthorizationGuard;

    public OperationalLogPruningController(
            OperationalLogPruningService operationalLogPruningService,
            OperationalLogPruningPolicy operationalLogPruningPolicy,
            AdminAuthorizationGuard adminAuthorizationGuard
    ) {
        this.operationalLogPruningService = operationalLogPruningService;
        this.operationalLogPruningPolicy = operationalLogPruningPolicy;
        this.adminAuthorizationGuard = adminAuthorizationGuard;
    }

    @PostMapping
    public ResponseEntity<OperationalLogPruningResult> prune(
            @RequestHeader(name = AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, required = false) String adminToken,
            @RequestHeader(name = AdminAuthorizationGuard.OPERATOR_ID_HEADER, required = false) String operatorId
    ) {
        adminAuthorizationGuard.authenticate(adminToken, operatorId);
        return ResponseEntity.ok(operationalLogPruningService.prune(
                operationalLogPruningPolicy.relayRunRetention(),
                operationalLogPruningPolicy.adminAccessAuditRetention()
        ));
    }
}
