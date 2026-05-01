package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.WalletLedgerQueryService;
import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationStepLog;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WalletLedgerController {

    private final WalletLedgerQueryService walletLedgerQueryService;

    public WalletLedgerController(WalletLedgerQueryService walletLedgerQueryService) {
        this.walletLedgerQueryService = walletLedgerQueryService;
    }

    @GetMapping("/wallets/{walletId}/ledger-entries")
    public List<LedgerEntry> ledgerEntries(@PathVariable String walletId) {
        return walletLedgerQueryService.getLedgerEntries(walletId);
    }

    @GetMapping("/audit-events")
    public List<AuditEvent> auditEvents() {
        return walletLedgerQueryService.getAuditEvents();
    }

    @GetMapping("/operations/{operationId}/step-logs")
    public List<OperationStepLog> operationStepLogs(@PathVariable String operationId) {
        return walletLedgerQueryService.getOperationStepLogs(operationId);
    }

    @GetMapping("/operations/{operationId}/outbox-events")
    public List<OperationOutboxEvent> operationOutboxEvents(@PathVariable String operationId) {
        return walletLedgerQueryService.getOperationOutboxEvents(operationId);
    }
}
