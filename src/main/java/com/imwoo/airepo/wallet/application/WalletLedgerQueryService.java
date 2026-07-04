package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.AuditEvent;
import com.imwoo.airepo.wallet.domain.LedgerEntry;
import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationStepLog;
import java.util.List;

public interface WalletLedgerQueryService {

    List<LedgerEntry> getLedgerEntries(String memberId, String walletId);

    List<AuditEvent> getAuditEvents();

    List<AuditEvent> getAuditEvents(String memberId, String walletId);

    List<OperationStepLog> getOperationStepLogs(String operationId);

    List<OperationOutboxEvent> getOperationOutboxEvents(String operationId);
}
