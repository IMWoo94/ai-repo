package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueAudit;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueRequestRecord;
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

    public OperationOutboxReviewController(OperationOutboxRelayService operationOutboxRelayService) {
        this.operationOutboxRelayService = operationOutboxRelayService;
    }

    @GetMapping("/manual-review")
    public List<OperationOutboxEvent> manualReviewEvents(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return operationOutboxRelayService.getManualReviewEvents(limit);
    }

    @PostMapping("/{outboxEventId}/requeue")
    public ResponseEntity<Void> requeueManualReviewEvent(
            @RequestHeader(name = AdminAuthorizationGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @PathVariable String outboxEventId,
            @RequestBody OperationOutboxRequeueRequest request
    ) {
        operationOutboxRelayService.requeueManualReviewEvent(outboxEventId, operatorId.trim(), request.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{outboxEventId}/requeue-audits")
    public List<OperationOutboxRequeueAudit> requeueAudits(
            @PathVariable String outboxEventId
    ) {
        return operationOutboxRelayService.getRequeueAudits(outboxEventId);
    }

    @GetMapping("/{outboxEventId}/requeue-requests")
    public List<OperationOutboxRequeueRequestRecord> requeueRequests(
            @PathVariable String outboxEventId
    ) {
        return operationOutboxRelayService.getRequeueRequests(outboxEventId);
    }

    @PostMapping("/{outboxEventId}/requeue-requests")
    public OperationOutboxRequeueRequestRecord requestRequeue(
            @RequestHeader(name = AdminAuthorizationGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @PathVariable String outboxEventId,
            @RequestBody OperationOutboxRequeueRequest request
    ) {
        return operationOutboxRelayService.requestManualReviewRequeue(outboxEventId, operatorId.trim(), request.reason());
    }

    @PostMapping("/requeue-requests/{requestId}/approve")
    public OperationOutboxRequeueRequestRecord approveRequeueRequest(
            @RequestHeader(name = AdminAuthorizationGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @PathVariable String requestId,
            @RequestBody OperationOutboxRequeueApprovalRequest request
    ) {
        return operationOutboxRelayService.approveManualReviewRequeueRequest(requestId, operatorId.trim(), request.reason());
    }

    @PostMapping("/requeue-requests/{requestId}/execute")
    public OperationOutboxRequeueRequestRecord executeRequeueRequest(
            @RequestHeader(name = AdminAuthorizationGuard.OPERATOR_ID_HEADER, required = false) String operatorId,
            @PathVariable String requestId
    ) {
        return operationOutboxRelayService.executeManualReviewRequeueRequest(requestId, operatorId.trim());
    }
}
