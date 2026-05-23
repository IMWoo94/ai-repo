package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationalAlert;
import com.imwoo.airepo.wallet.domain.OperationalAlertSeverity;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class OperationalAlertService {

    private static final int MAX_ALERT_LIMIT = 100;

    private final OperationalAlertRepository operationalAlertRepository;
    private final OperationalAlertPolicy operationalAlertPolicy;
    private final OperationalAlertPublisher operationalAlertPublisher;

    public OperationalAlertService(
            OperationalAlertRepository operationalAlertRepository,
            OperationalAlertPolicy operationalAlertPolicy,
            OperationalAlertPublisher operationalAlertPublisher
    ) {
        this.operationalAlertRepository = operationalAlertRepository;
        this.operationalAlertPolicy = operationalAlertPolicy;
        this.operationalAlertPublisher = operationalAlertPublisher;
    }

    public synchronized void publishHealthAlert(String source, String status, Instant occurredAt, List<String> reasons) {
        if (reasons.isEmpty()) {
            return;
        }
        OperationalAlertSeverity severity = severity(status);
        if (severity == null) {
            return;
        }
        List<String> alertReasons = List.copyOf(reasons);
        if (operationalAlertRepository.existsOperationalAlertBetween(
                source,
                severity,
                alertReasons,
                occurredAt.minus(operationalAlertPolicy.suppressionWindow()),
                occurredAt
        )) {
            return;
        }
        OperationalAlert operationalAlert = new OperationalAlert(
                operationalAlertRepository.nextOperationalAlertId(),
                source,
                severity,
                occurredAt,
                alertReasons
        );
        operationalAlertRepository.saveOperationalAlert(operationalAlert);
        publishExternalAlert(operationalAlert);
    }

    public List<OperationalAlert> findRecentAlerts(int limit) {
        if (limit < 1 || limit > MAX_ALERT_LIMIT) {
            throw new InvalidWalletOperationException("limit must be between 1 and 100");
        }
        return operationalAlertRepository.findRecentOperationalAlerts(limit);
    }

    private OperationalAlertSeverity severity(String status) {
        String normalizedStatus = status.toUpperCase(Locale.ROOT);
        if (OperationalAlertSeverity.WARNING.name().equals(normalizedStatus)) {
            return OperationalAlertSeverity.WARNING;
        }
        if (OperationalAlertSeverity.CRITICAL.name().equals(normalizedStatus)) {
            return OperationalAlertSeverity.CRITICAL;
        }
        return null;
    }

    private void publishExternalAlert(OperationalAlert operationalAlert) {
        try {
            operationalAlertPublisher.publish(operationalAlert);
        } catch (RuntimeException ignored) {
            // Alert records are the local source of truth. External notification failures must not fail health APIs.
        }
    }
}
