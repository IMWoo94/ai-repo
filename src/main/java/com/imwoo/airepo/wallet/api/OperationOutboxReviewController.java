package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueAudit;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outbox-events")
public class OperationOutboxReviewController {

    private final OperationOutboxRelayService operationOutboxRelayService;
    private final AdminAuthorizationGuard adminAuthorizationGuard;

    public OperationOutboxReviewController(
            OperationOutboxRelayService operationOutboxRelayService,
            AdminAuthorizationGuard adminAuthorizationGuard
    ) {
        this.operationOutboxRelayService = operationOutboxRelayService;
        this.adminAuthorizationGuard = adminAuthorizationGuard;
    }

    @GetMapping("/manual-review")
    public List<OperationOutboxEvent> manualReviewEvents(
            @RequestHeader(name = AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, required = false) String adminToken,
            @RequestHeader(name = AdminAuthorizationGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        adminAuthorizationGuard.authenticate(adminToken, operatorId);
        return operationOutboxRelayService.getManualReviewEvents(limit);
    }

    @PostMapping("/{outboxEventId}/requeue")
    public ResponseEntity<Void> requeueManualReviewEvent(
            @RequestHeader(name = AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, required = false) String adminToken,
            @RequestHeader(name = AdminAuthorizationGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @PathVariable String outboxEventId,
            @RequestBody OperationOutboxRequeueRequest request
    ) {
        AdminOperator operator = adminAuthorizationGuard.authenticate(adminToken, operatorId);
        operationOutboxRelayService.requeueManualReviewEvent(outboxEventId, operator.operatorId(), request.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{outboxEventId}/requeue-audits")
    public List<OperationOutboxRequeueAudit> requeueAudits(
            @RequestHeader(name = AdminAuthorizationGuard.ADMIN_TOKEN_HEADER, required = false) String adminToken,
            @RequestHeader(name = AdminAuthorizationGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @PathVariable String outboxEventId
    ) {
        adminAuthorizationGuard.authenticate(adminToken, operatorId);
        return operationOutboxRelayService.getRequeueAudits(outboxEventId);
    }
}
