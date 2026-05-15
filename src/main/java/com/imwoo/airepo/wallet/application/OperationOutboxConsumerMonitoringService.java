package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OperationOutboxConsumerMonitoringService {

    private static final int MAX_RECEIPT_LIMIT = 100;
    private static final int MAX_WINDOW_MINUTES = 1440;

    private final OperationOutboxConsumerMonitoringRepository monitoringRepository;
    private final OperationOutboxConsumerHealthPolicy healthPolicy;
    private final Clock clock;

    public OperationOutboxConsumerMonitoringService(
            OperationOutboxConsumerMonitoringRepository monitoringRepository,
            OperationOutboxConsumerHealthPolicy healthPolicy,
            Clock clock
    ) {
        this.monitoringRepository = monitoringRepository;
        this.healthPolicy = healthPolicy;
        this.clock = clock;
    }

    public OperationOutboxConsumerMetrics getMetrics() {
        return monitoringRepository.getConsumerMetrics();
    }

    public OperationOutboxConsumerWindowMetrics getWindowMetrics(int minutes) {
        if (minutes < 1 || minutes > MAX_WINDOW_MINUTES) {
            throw new InvalidWalletOperationException("minutes must be between 1 and 1440");
        }
        Instant windowEndedAt = Instant.now(clock).truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES);
        Instant windowStartedAt = windowEndedAt.minus(minutes, ChronoUnit.MINUTES);
        return monitoringRepository.getConsumerWindowMetrics(windowStartedAt, windowEndedAt);
    }

    public List<OperationOutboxConsumerReceipt> findRecentReceipts(int limit) {
        if (limit < 1 || limit > MAX_RECEIPT_LIMIT) {
            throw new InvalidWalletOperationException("limit must be between 1 and 100");
        }
        return monitoringRepository.findRecentConsumerReceipts(limit);
    }

    public OperationOutboxConsumerHealthSummary getHealthSummary() {
        OperationOutboxConsumerMetrics metrics = monitoringRepository.getConsumerMetrics();
        Instant evaluatedAt = Instant.now(clock);
        OperationOutboxConsumerWindowMetrics windowMetrics = getWindowMetrics(Math.toIntExact(healthPolicy.windowMinutes()));
        long totalDeliveryCount = metrics.processedEventCount() + metrics.duplicateEventCount();
        double duplicateRate = totalDeliveryCount == 0 ? 0.0 : (double) metrics.duplicateEventCount() / totalDeliveryCount;
        if (windowMetrics.totalDeliveryCount() == 0) {
            return new OperationOutboxConsumerHealthSummary(
                    evaluatedAt,
                    OperationOutboxConsumerHealthStatus.NO_DATA,
                    metrics.processedEventCount(),
                    metrics.duplicateEventCount(),
                    metrics.receiptCount(),
                    duplicateRate,
                    windowMetrics.windowStartedAt(),
                    windowMetrics.windowEndedAt(),
                    windowMetrics.processedDeliveryCount(),
                    windowMetrics.duplicateDeliveryCount(),
                    windowMetrics.duplicateRate(),
                    healthPolicy.windowMinutes(),
                    healthPolicy.minDuplicateEventCount(),
                    healthPolicy.warningDuplicateRate(),
                    healthPolicy.criticalDuplicateRate(),
                    List.of("no consumer event data in health window")
            );
        }
        List<String> alertReasons = alertReasons(windowMetrics.duplicateDeliveryCount(), windowMetrics.duplicateRate());
        return new OperationOutboxConsumerHealthSummary(
                evaluatedAt,
                status(alertReasons),
                metrics.processedEventCount(),
                metrics.duplicateEventCount(),
                metrics.receiptCount(),
                duplicateRate,
                windowMetrics.windowStartedAt(),
                windowMetrics.windowEndedAt(),
                windowMetrics.processedDeliveryCount(),
                windowMetrics.duplicateDeliveryCount(),
                windowMetrics.duplicateRate(),
                healthPolicy.windowMinutes(),
                healthPolicy.minDuplicateEventCount(),
                healthPolicy.warningDuplicateRate(),
                healthPolicy.criticalDuplicateRate(),
                alertReasons
        );
    }

    private List<String> alertReasons(long duplicateEventCount, double duplicateRate) {
        List<String> alertReasons = new ArrayList<>();
        if (duplicateEventCount < healthPolicy.minDuplicateEventCount()) {
            return alertReasons;
        }
        if (duplicateRate >= healthPolicy.criticalDuplicateRate()) {
            alertReasons.add("critical consumer duplicate delivery rate in health window");
        } else if (duplicateRate >= healthPolicy.warningDuplicateRate()) {
            alertReasons.add("warning consumer duplicate delivery rate in health window");
        }
        return alertReasons;
    }

    private OperationOutboxConsumerHealthStatus status(List<String> alertReasons) {
        if (alertReasons.isEmpty()) {
            return OperationOutboxConsumerHealthStatus.OK;
        }
        if (alertReasons.stream().anyMatch(alertReason -> alertReason.startsWith("critical"))) {
            return OperationOutboxConsumerHealthStatus.CRITICAL;
        }
        return OperationOutboxConsumerHealthStatus.WARNING;
    }
}
