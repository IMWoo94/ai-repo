package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.OperationOutboxConsumerPruningPolicy;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerPruningResult;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerPruningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/outbox-consumer/pruning-runs")
public class OperationOutboxConsumerPruningController {

    private final OperationOutboxConsumerPruningService pruningService;
    private final OperationOutboxConsumerPruningPolicy pruningPolicy;

    public OperationOutboxConsumerPruningController(
            OperationOutboxConsumerPruningService pruningService,
            OperationOutboxConsumerPruningPolicy pruningPolicy
    ) {
        this.pruningService = pruningService;
        this.pruningPolicy = pruningPolicy;
    }

    @PostMapping
    public ResponseEntity<OperationOutboxConsumerPruningResult> prune() {
        return ResponseEntity.ok(pruningService.prune(
                pruningPolicy.processedEventRetention(),
                pruningPolicy.receiptRetention(),
                pruningPolicy.deliveryMetricRetention()
        ));
    }
}
