package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import com.imwoo.airepo.wallet.domain.OperationStepLog;
import java.util.List;

public interface WalletLedgerQueryService {

    List<LedgerEntry> getLedgerEntries(String walletId);

    List<AuditEvent> getAuditEvents();

    List<OperationStepLog> getOperationStepLogs(String operationId);
}
