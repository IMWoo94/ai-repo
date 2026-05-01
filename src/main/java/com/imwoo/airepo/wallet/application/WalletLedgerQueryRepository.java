package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import com.imwoo.airepo.wallet.domain.OperationStepLog;
import java.util.List;

public interface WalletLedgerQueryRepository {

    List<LedgerEntry> findLedgerEntries(String walletId);

    List<AuditEvent> findAuditEvents();

    List<OperationStepLog> findOperationStepLogs(String operationId);
}
