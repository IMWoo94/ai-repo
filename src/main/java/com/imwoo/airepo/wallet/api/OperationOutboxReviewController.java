package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public List<OperationOutboxEvent> manualReviewEvents(@RequestParam(defaultValue = "50") int limit) {
        return operationOutboxRelayService.getManualReviewEvents(limit);
    }

    @PostMapping("/{outboxEventId}/requeue")
    public ResponseEntity<Void> requeueManualReviewEvent(@PathVariable String outboxEventId) {
        operationOutboxRelayService.requeueManualReviewEvent(outboxEventId);
        return ResponseEntity.noContent().build();
    }
}
