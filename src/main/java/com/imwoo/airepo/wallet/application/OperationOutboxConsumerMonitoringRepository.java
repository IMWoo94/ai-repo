package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
import java.time.Instant;
import java.util.List;

public interface OperationOutboxConsumerMonitoringRepository {

    OperationOutboxConsumerMetrics getConsumerMetrics();

    OperationOutboxConsumerWindowMetrics getConsumerWindowMetrics(Instant windowStartedAt, Instant windowEndedAt);

    List<OperationOutboxConsumerReceipt> findRecentConsumerReceipts(int limit);
}
