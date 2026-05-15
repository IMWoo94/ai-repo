package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
import java.util.List;

public interface OperationOutboxConsumerMonitoringRepository {

    OperationOutboxConsumerMetrics getConsumerMetrics();

    List<OperationOutboxConsumerReceipt> findRecentConsumerReceipts(int limit);
}
