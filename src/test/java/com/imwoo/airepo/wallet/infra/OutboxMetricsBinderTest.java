package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.imwoo.airepo.wallet.application.OperationOutboxConsumerHealthStatus;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerHealthSummary;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerMetrics;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerMonitoringService;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayMonitoringService;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import com.imwoo.airepo.wallet.application.OutboxRelayHealthStatus;
import com.imwoo.airepo.wallet.application.OutboxRelayHealthSummary;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutboxMetricsBinderTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final OperationOutboxRelayService relayService = mock(OperationOutboxRelayService.class);
    private final OperationOutboxRelayMonitoringService relayMonitoringService =
            mock(OperationOutboxRelayMonitoringService.class);
    private final OperationOutboxConsumerMonitoringService consumerMonitoringService =
            mock(OperationOutboxConsumerMonitoringService.class);
    private final OutboxMetricsBinder binder = new OutboxMetricsBinder(
            relayService,
            relayMonitoringService,
            consumerMonitoringService
    );

    @Test
    void exposesPendingGaugeReadingRelayService() {
        when(relayService.getPendingEventCount()).thenReturn(7L);
        binder.bindTo(registry);

        assertThat(registry.get(OutboxMetricsBinder.RELAY_PENDING_METRIC).gauge().value()).isEqualTo(7.0);

        when(relayService.getPendingEventCount()).thenReturn(3L);
        assertThat(registry.get(OutboxMetricsBinder.RELAY_PENDING_METRIC).gauge().value()).isEqualTo(3.0);
    }

    @Test
    void mapsRelayHealthStatusToNumericGauge() {
        when(relayMonitoringService.getHealthSummary()).thenReturn(healthSummary(OutboxRelayHealthStatus.CRITICAL));
        binder.bindTo(registry);

        assertThat(registry.get(OutboxMetricsBinder.RELAY_HEALTH_METRIC).gauge().value()).isEqualTo(2.0);

        when(relayMonitoringService.getHealthSummary()).thenReturn(healthSummary(OutboxRelayHealthStatus.WARNING));
        assertThat(registry.get(OutboxMetricsBinder.RELAY_HEALTH_METRIC).gauge().value()).isEqualTo(1.0);

        when(relayMonitoringService.getHealthSummary()).thenReturn(healthSummary(OutboxRelayHealthStatus.OK));
        assertThat(registry.get(OutboxMetricsBinder.RELAY_HEALTH_METRIC).gauge().value()).isEqualTo(0.0);
    }

    @Test
    void mapsConsumerHealthStatusToNumericGauge() {
        when(consumerMonitoringService.getHealthSummary())
                .thenReturn(consumerHealthSummary(OperationOutboxConsumerHealthStatus.CRITICAL));
        binder.bindTo(registry);

        assertThat(registry.get(OutboxMetricsBinder.CONSUMER_HEALTH_METRIC).gauge().value()).isEqualTo(2.0);

        when(consumerMonitoringService.getHealthSummary())
                .thenReturn(consumerHealthSummary(OperationOutboxConsumerHealthStatus.OK));
        assertThat(registry.get(OutboxMetricsBinder.CONSUMER_HEALTH_METRIC).gauge().value()).isEqualTo(0.0);
    }

    @Test
    void exposesConsumerEventCountersByOutcome() {
        when(consumerMonitoringService.getMetrics()).thenReturn(consumerMetrics(5, 2));
        binder.bindTo(registry);

        assertThat(consumerEventsCounter("processed")).isEqualTo(5.0);
        assertThat(consumerEventsCounter("duplicate")).isEqualTo(2.0);

        when(consumerMonitoringService.getMetrics()).thenReturn(consumerMetrics(8, 3));
        assertThat(consumerEventsCounter("processed")).isEqualTo(8.0);
        assertThat(consumerEventsCounter("duplicate")).isEqualTo(3.0);
    }

    private double consumerEventsCounter(String outcome) {
        return registry.get(OutboxMetricsBinder.CONSUMER_EVENTS_METRIC)
                .tag("outcome", outcome).functionCounter().count();
    }

    private OutboxRelayHealthSummary healthSummary(OutboxRelayHealthStatus status) {
        return new OutboxRelayHealthSummary(
                Instant.parse("2026-05-01T00:00:00Z"),
                status,
                5, 5, 4, 1, 0.2, 0,
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:00Z"),
                null,
                List.of()
        );
    }

    private OperationOutboxConsumerHealthSummary consumerHealthSummary(OperationOutboxConsumerHealthStatus status) {
        return new OperationOutboxConsumerHealthSummary(
                Instant.parse("2026-05-01T00:00:00Z"),
                status,
                10, 1, 10, 0.09,
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:05:00Z"),
                10, 1, 0.09,
                5, 5, 0.2, 0.5,
                List.of()
        );
    }

    private OperationOutboxConsumerMetrics consumerMetrics(long processed, long duplicate) {
        return new OperationOutboxConsumerMetrics(
                processed,
                duplicate,
                processed,
                Instant.parse("2026-05-01T00:05:00Z"),
                Instant.parse("2026-05-01T00:05:00Z")
        );
    }
}
