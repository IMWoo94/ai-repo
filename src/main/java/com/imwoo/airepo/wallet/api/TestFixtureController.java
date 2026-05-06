package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import com.imwoo.airepo.wallet.application.WalletChargeCommand;
import com.imwoo.airepo.wallet.application.WalletCommandResult;
import com.imwoo.airepo.wallet.application.WalletCommandService;
import com.imwoo.airepo.wallet.application.WalletLedgerQueryService;
import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test-fixtures")
@ConditionalOnProperty(prefix = "ai-repo.test-fixtures", name = "enabled", havingValue = "true")
public class TestFixtureController {

    private final WalletCommandService walletCommandService;
    private final WalletLedgerQueryService walletLedgerQueryService;
    private final OperationOutboxRelayService operationOutboxRelayService;

    public TestFixtureController(
            WalletCommandService walletCommandService,
            WalletLedgerQueryService walletLedgerQueryService,
            OperationOutboxRelayService operationOutboxRelayService
    ) {
        this.walletCommandService = walletCommandService;
        this.walletLedgerQueryService = walletLedgerQueryService;
        this.operationOutboxRelayService = operationOutboxRelayService;
    }

    @PostMapping("/outbox-events/manual-review")
    public ResponseEntity<ManualReviewFixtureResponse> createManualReviewFixture() {
        WalletCommandResult commandResult = walletCommandService.charge(
                "wallet-001",
                new WalletChargeCommand(
                        new Money(BigDecimal.ONE, "KRW"),
                        "e2e-manual-review-fixture-" + System.nanoTime(),
                        "E2E manual review fixture"
                )
        );
        List<OperationOutboxEvent> outboxEvents = walletLedgerQueryService.getOperationOutboxEvents(
                commandResult.operation().operationId()
        );
        if (outboxEvents.isEmpty()) {
            throw new IllegalStateException("manual review fixture outbox event was not created");
        }
        String outboxEventId = outboxEvents.getFirst().outboxEventId();
        operationOutboxRelayService.markFailed(outboxEventId, "e2e broker unavailable");
        operationOutboxRelayService.markFailed(outboxEventId, "e2e broker unavailable");
        operationOutboxRelayService.markFailed(outboxEventId, "e2e broker unavailable");
        return ResponseEntity.ok(new ManualReviewFixtureResponse(
                commandResult.operation().operationId(),
                outboxEventId
        ));
    }

    public record ManualReviewFixtureResponse(String operationId, String outboxEventId) {
    }
}
