package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
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

    private void validateWalletId(String walletId) {
        if (walletId == null || walletId.isBlank()) {
            throw new InvalidWalletIdException("walletId must not be blank");
        }
    }
}
