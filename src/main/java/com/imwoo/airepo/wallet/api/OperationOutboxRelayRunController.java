package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.OperationOutboxRelayMonitoringService;
import com.imwoo.airepo.wallet.application.OutboxRelayHealthSummary;
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

    public OperationOutboxRelayRunController(OperationOutboxRelayMonitoringService operationOutboxRelayMonitoringService) {
        this.operationOutboxRelayMonitoringService = operationOutboxRelayMonitoringService;
    }

    @GetMapping
    public List<OperationOutboxRelayRun> recentRuns(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return operationOutboxRelayMonitoringService.getRecentRuns(limit);
    }

    @GetMapping("/health")
    public OutboxRelayHealthSummary health() {
        return operationOutboxRelayMonitoringService.getHealthSummary();
    }
}
