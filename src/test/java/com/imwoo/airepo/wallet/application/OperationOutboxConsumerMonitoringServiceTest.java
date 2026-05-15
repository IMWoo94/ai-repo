package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OperationOutboxConsumerMonitoringServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final OperationOutboxConsumerMonitoringService monitoringService = new OperationOutboxConsumerMonitoringService(
            repository,
            new OperationOutboxConsumerHealthPolicy(1, 20, 50, 5),
            new OperationalAlertService(repository),
            Clock.fixed(Instant.parse("2026-05-01T00:20:00Z"), ZoneOffset.UTC)
    );

    @Test
    void returnsNoDataHealthSummaryWhenConsumerEventDoesNotExist() {
        OperationOutboxConsumerHealthSummary summary = monitoringService.getHealthSummary();

        assertThat(summary.status()).isEqualTo(OperationOutboxConsumerHealthStatus.NO_DATA);
        assertThat(summary.duplicateRate()).isZero();
        assertThat(summary.windowStartedAt()).isEqualTo(Instant.parse("2026-05-01T00:16:00Z"));
        assertThat(summary.windowEndedAt()).isEqualTo(Instant.parse("2026-05-01T00:21:00Z"));
        assertThat(summary.windowDuplicateRate()).isZero();
        assertThat(summary.alertReasons()).containsExactly("no consumer event data in health window");
        assertThat(repository.findRecentOperationalAlerts(10)).isEmpty();
    }

    @Test
    void returnsOkHealthSummaryWhenDuplicateRateIsBelowThreshold() {
        recordProcessed("outbox-001");
        recordProcessed("outbox-002");

        OperationOutboxConsumerHealthSummary summary = monitoringService.getHealthSummary();

        assertThat(summary.status()).isEqualTo(OperationOutboxConsumerHealthStatus.OK);
        assertThat(summary.processedEventCount()).isEqualTo(2);
        assertThat(summary.duplicateEventCount()).isZero();
        assertThat(summary.duplicateRate()).isZero();
        assertThat(summary.windowProcessedDeliveryCount()).isEqualTo(2);
        assertThat(summary.windowDuplicateDeliveryCount()).isZero();
        assertThat(summary.windowDuplicateRate()).isZero();
        assertThat(summary.alertReasons()).isEmpty();
        assertThat(repository.findRecentOperationalAlerts(10)).isEmpty();
    }

    @Test
    void returnsWarningWhenDuplicateRateReachesWarningThreshold() {
        recordProcessed("outbox-001");
        recordDuplicate("outbox-001");
        recordProcessed("outbox-002");
        recordProcessed("outbox-003");
        recordProcessed("outbox-004");

        OperationOutboxConsumerHealthSummary summary = monitoringService.getHealthSummary();

        assertThat(summary.status()).isEqualTo(OperationOutboxConsumerHealthStatus.WARNING);
        assertThat(summary.processedEventCount()).isEqualTo(4);
        assertThat(summary.duplicateEventCount()).isEqualTo(1);
        assertThat(summary.duplicateRate()).isEqualTo(0.2);
        assertThat(summary.windowProcessedDeliveryCount()).isEqualTo(4);
        assertThat(summary.windowDuplicateDeliveryCount()).isEqualTo(1);
        assertThat(summary.windowDuplicateRate()).isEqualTo(0.2);
        assertThat(summary.alertReasons()).containsExactly("warning consumer duplicate delivery rate in health window");
        assertThat(repository.findRecentOperationalAlerts(10))
                .singleElement()
                .satisfies(alert -> {
                    assertThat(alert.alertId()).isEqualTo("operational-alert-001");
                    assertThat(alert.source()).isEqualTo("OUTBOX_CONSUMER");
                    assertThat(alert.severity().name()).isEqualTo("WARNING");
                    assertThat(alert.reasons()).containsExactly("warning consumer duplicate delivery rate in health window");
                });
    }

    @Test
    void returnsCriticalWhenDuplicateRateReachesCriticalThreshold() {
        recordProcessed("outbox-001");
        recordDuplicate("outbox-001");
        recordDuplicate("outbox-001");

        OperationOutboxConsumerHealthSummary summary = monitoringService.getHealthSummary();

        assertThat(summary.status()).isEqualTo(OperationOutboxConsumerHealthStatus.CRITICAL);
        assertThat(summary.processedEventCount()).isEqualTo(1);
        assertThat(summary.duplicateEventCount()).isEqualTo(2);
        assertThat(summary.duplicateRate()).isEqualTo(2.0 / 3.0);
        assertThat(summary.windowProcessedDeliveryCount()).isEqualTo(1);
        assertThat(summary.windowDuplicateDeliveryCount()).isEqualTo(2);
        assertThat(summary.windowDuplicateRate()).isEqualTo(2.0 / 3.0);
        assertThat(summary.alertReasons()).containsExactly("critical consumer duplicate delivery rate in health window");
        assertThat(repository.findRecentOperationalAlerts(10))
                .singleElement()
                .satisfies(alert -> {
                    assertThat(alert.source()).isEqualTo("OUTBOX_CONSUMER");
                    assertThat(alert.severity().name()).isEqualTo("CRITICAL");
                    assertThat(alert.reasons()).containsExactly("critical consumer duplicate delivery rate in health window");
                });
    }

    @Test
    void ignoresOlderDuplicateMetricsOutsideHealthWindow() {
        repository.recordProcessedEvent(
                "outbox-old",
                "outbox-old",
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:01:00Z")
        );
        repository.recordConsumerDeliveryMetric(Instant.parse("2026-05-01T00:01:00Z"), false);
        repository.recordProcessedEvent(
                "outbox-old",
                "outbox-old",
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:02:00Z")
        );
        repository.recordConsumerDeliveryMetric(Instant.parse("2026-05-01T00:02:00Z"), true);
        recordProcessed("outbox-001");

        OperationOutboxConsumerHealthSummary summary = monitoringService.getHealthSummary();

        assertThat(summary.status()).isEqualTo(OperationOutboxConsumerHealthStatus.OK);
        assertThat(summary.duplicateEventCount()).isEqualTo(1);
        assertThat(summary.windowProcessedDeliveryCount()).isEqualTo(1);
        assertThat(summary.windowDuplicateDeliveryCount()).isZero();
        assertThat(summary.windowDuplicateRate()).isZero();
    }

    @Test
    void returnsNoDataWhenOnlyOlderMetricsExistOutsideHealthWindow() {
        repository.recordProcessedEvent(
                "outbox-old",
                "outbox-old",
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:01:00Z")
        );
        repository.recordConsumerDeliveryMetric(Instant.parse("2026-05-01T00:01:00Z"), false);
        repository.recordProcessedEvent(
                "outbox-old",
                "outbox-old",
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:02:00Z")
        );
        repository.recordConsumerDeliveryMetric(Instant.parse("2026-05-01T00:02:00Z"), true);

        OperationOutboxConsumerHealthSummary summary = monitoringService.getHealthSummary();

        assertThat(summary.status()).isEqualTo(OperationOutboxConsumerHealthStatus.NO_DATA);
        assertThat(summary.duplicateRate()).isEqualTo(0.5);
        assertThat(summary.windowDuplicateRate()).isZero();
        assertThat(summary.alertReasons()).containsExactly("no consumer event data in health window");
    }

    private void recordProcessed(String outboxEventId) {
        repository.recordProcessedEvent(
                outboxEventId,
                outboxEventId,
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:17:00Z")
        );
        repository.recordConsumerDeliveryMetric(Instant.parse("2026-05-01T00:17:00Z"), false);
    }

    private void recordDuplicate(String outboxEventId) {
        repository.recordProcessedEvent(
                outboxEventId,
                outboxEventId,
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:18:00Z")
        );
        repository.recordConsumerDeliveryMetric(Instant.parse("2026-05-01T00:18:00Z"), true);
    }
}
