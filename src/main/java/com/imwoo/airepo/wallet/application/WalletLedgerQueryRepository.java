package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import java.util.List;

public interface WalletLedgerQueryRepository {

    List<LedgerEntry> findLedgerEntries(String walletId);

    List<AuditEvent> findAuditEvents();
}
