package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.OperationOutboxConsumerHealthSummary;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerMetrics;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerMonitoringService;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerWindowMetrics;
import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outbox-consumer")
public class OperationOutboxConsumerMonitoringController {

    private final OperationOutboxConsumerMonitoringService monitoringService;

    public OperationOutboxConsumerMonitoringController(OperationOutboxConsumerMonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/metrics")
    public OperationOutboxConsumerMetrics getMetrics() {
        return monitoringService.getMetrics();
    }

    @GetMapping("/window-metrics")
    public OperationOutboxConsumerWindowMetrics getWindowMetrics(
            @RequestParam(defaultValue = "5") int minutes
    ) {
        return monitoringService.getWindowMetrics(minutes);
    }

    @GetMapping("/health")
    public OperationOutboxConsumerHealthSummary getHealth() {
        return monitoringService.getHealthSummary();
    }

    @GetMapping("/receipts")
    public List<OperationOutboxConsumerReceipt> findRecentReceipts(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return monitoringService.findRecentReceipts(limit);
    }
}
