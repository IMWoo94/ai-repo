package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import java.util.List;

public interface WalletLedgerQueryService {

    List<LedgerEntry> getLedgerEntries(String walletId);

    List<AuditEvent> getAuditEvents();
}
