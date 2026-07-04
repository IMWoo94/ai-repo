package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.imwoo.airepo.wallet.application.OperationalAlertPolicy;
import com.imwoo.airepo.wallet.application.OperationalAlertService;
import com.imwoo.airepo.wallet.application.OperationOutboxPublishBatchResult;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayMonitoringService;
import com.imwoo.airepo.wallet.application.OutboxRelayHealthPolicy;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OutboxRelayMetricsRegistryTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final OutboxRelayMetricsRegistry metricsRegistry = new OutboxRelayMetricsRegistry(registry);
    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final OperationOutboxRelayMonitoringService monitoringService = new OperationOutboxRelayMonitoringService(
            repository,
            new OutboxRelayHealthPolicy(5, 2, 3, 50, 15),
            new OperationalAlertService(repository, new OperationalAlertPolicy(15, 30), operationalAlert -> {
            }),
            metricsRegistry,
            Clock.fixed(Instant.parse("2026-05-01T00:20:00Z"), ZoneOffset.UTC)
    );

    @Test
    void countsSuccessfulRelayRunsAndPublishedEvents() {
        monitoringService.recordSuccess(
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:01Z"),
                10,
                new OperationOutboxPublishBatchResult(3, 2, 1)
        );

        assertThat(runsCounter("success")).isEqualTo(1.0);
        assertThat(runsCounter("failure")).isEqualTo(0.0);
        assertThat(publishedCounter("published")).isEqualTo(2.0);
        assertThat(publishedCounter("failed")).isEqualTo(1.0);
    }

    @Test
    void countsFailedRelayRuns() {
        monitoringService.recordFailure(
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:01Z"),
                10,
                "publisher down"
        );

        assertThat(runsCounter("failure")).isEqualTo(1.0);
        assertThat(runsCounter("success")).isEqualTo(0.0);
        assertThat(publishedCounter("published")).isEqualTo(0.0);
    }

    private double runsCounter(String result) {
        return registry.get(OutboxRelayMetricsRegistry.RELAY_RUNS_METRIC).tag("result", result).counter().count();
    }

    private double publishedCounter(String outcome) {
        return registry.get(OutboxRelayMetricsRegistry.RELAY_PUBLISHED_METRIC)
                .tag("outcome", outcome).counter().count();
    }
}
