package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OperationOutboxConsumerMonitoringService {

    private static final int MAX_RECEIPT_LIMIT = 100;

    private final OperationOutboxConsumerMonitoringRepository monitoringRepository;

    public OperationOutboxConsumerMonitoringService(OperationOutboxConsumerMonitoringRepository monitoringRepository) {
        this.monitoringRepository = monitoringRepository;
    }

    public OperationOutboxConsumerMetrics getMetrics() {
        return monitoringRepository.getConsumerMetrics();
    }

    public List<OperationOutboxConsumerReceipt> findRecentReceipts(int limit) {
        if (limit < 1 || limit > MAX_RECEIPT_LIMIT) {
            throw new InvalidWalletOperationException("limit must be between 1 and 100");
        }
        return monitoringRepository.findRecentConsumerReceipts(limit);
    }
}
