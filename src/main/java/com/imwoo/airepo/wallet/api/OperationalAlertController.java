package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.OperationalAlertService;
import com.imwoo.airepo.wallet.domain.OperationalAlert;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operational-alerts")
public class OperationalAlertController {

    private final OperationalAlertService operationalAlertService;

    public OperationalAlertController(OperationalAlertService operationalAlertService) {
        this.operationalAlertService = operationalAlertService;
    }

    @GetMapping
    public List<OperationalAlert> findRecentAlerts(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return operationalAlertService.findRecentAlerts(limit);
    }
}
