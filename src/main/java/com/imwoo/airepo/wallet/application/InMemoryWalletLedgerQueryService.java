package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationStepLog;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InMemoryWalletLedgerQueryService implements WalletLedgerQueryService {

    private final WalletQueryRepository walletQueryRepository;
    private final WalletLedgerQueryRepository walletLedgerQueryRepository;

    public InMemoryWalletLedgerQueryService(
            WalletQueryRepository walletQueryRepository,
            WalletLedgerQueryRepository walletLedgerQueryRepository
    ) {
        this.walletQueryRepository = walletQueryRepository;
        this.walletLedgerQueryRepository = walletLedgerQueryRepository;
    }

    @Override
    public List<LedgerEntry> getLedgerEntries(String walletId) {
        validateWalletId(walletId);
        walletQueryRepository.findWalletAccount(walletId)
                .orElseThrow(() -> new WalletNotFoundException(walletId));
        return walletLedgerQueryRepository.findLedgerEntries(walletId).stream()
                .sorted(Comparator.comparing(LedgerEntry::occurredAt)
                        .thenComparing(LedgerEntry::ledgerEntryId)
                        .reversed())
                .toList();
    }

    @Override
    public List<AuditEvent> getAuditEvents() {
        return walletLedgerQueryRepository.findAuditEvents().stream()
                .sorted(Comparator.comparing(AuditEvent::occurredAt)
                        .thenComparing(AuditEvent::auditEventId)
                        .reversed())
                .toList();
    }

    @Override
    public List<OperationStepLog> getOperationStepLogs(String operationId) {
        validateOperationId(operationId);
        return walletLedgerQueryRepository.findOperationStepLogs(operationId).stream()
                .sorted(Comparator.comparing(OperationStepLog::occurredAt)
                        .thenComparing(OperationStepLog::operationStepLogId))
                .toList();
    }

    @Override
    public List<OperationOutboxEvent> getOperationOutboxEvents(String operationId) {
        validateOperationId(operationId);
        return walletLedgerQueryRepository.findOperationOutboxEvents(operationId).stream()
                .sorted(Comparator.comparing(OperationOutboxEvent::occurredAt)
                        .thenComparing(OperationOutboxEvent::outboxEventId))
                .toList();
    }

    private void validateWalletId(String walletId) {
        if (walletId == null || walletId.isBlank()) {
            throw new InvalidWalletIdException("walletId must not be blank");
        }
    }

    private void validateOperationId(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            throw new InvalidWalletOperationException("operationId must not be blank");
        }
    }
}
