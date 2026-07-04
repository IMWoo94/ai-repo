package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationOutboxConsumerHealthStatus;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerMetrics;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerMonitoringService;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayMonitoringService;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import com.imwoo.airepo.wallet.application.OutboxRelayHealthStatus;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/**
 * Non-invasive Micrometer bridge that exposes outbox relay and consumer metrics for Prometheus.
 *
 * <p>Gauges read the existing monitoring services on each scrape, so no service state is duplicated.
 * Consumer counters are {@link FunctionCounter}s over the cumulative consumer metrics (they advance
 * as {@code consume} runs). Relay run/publish counters live in {@link OutboxRelayMetricsRegistry},
 * which is driven from the relay monitoring service's record points.
 */
@Component
public class OutboxMetricsBinder implements MeterBinder {

    static final String RELAY_PENDING_METRIC = "ai.repo.outbox.relay.pending.events";
    static final String RELAY_HEALTH_METRIC = "ai.repo.outbox.relay.health.status";
    static final String CONSUMER_EVENTS_METRIC = "ai.repo.outbox.consumer.events";
    static final String CONSUMER_HEALTH_METRIC = "ai.repo.outbox.consumer.health.status";

    private final OperationOutboxRelayService relayService;
    private final OperationOutboxRelayMonitoringService relayMonitoringService;
    private final OperationOutboxConsumerMonitoringService consumerMonitoringService;

    public OutboxMetricsBinder(
            OperationOutboxRelayService relayService,
            OperationOutboxRelayMonitoringService relayMonitoringService,
            OperationOutboxConsumerMonitoringService consumerMonitoringService
    ) {
        this.relayService = relayService;
        this.relayMonitoringService = relayMonitoringService;
        this.consumerMonitoringService = consumerMonitoringService;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder(RELAY_PENDING_METRIC, relayService, service -> service.getPendingEventCount())
                .description("Pending (unpublished) outbox events awaiting relay")
                .register(registry);

        Gauge.builder(RELAY_HEALTH_METRIC, relayMonitoringService, this::relayHealthValue)
                .description("Outbox relay health status (0=ok/no-data, 1=warning, 2=critical)")
                .register(registry);

        Gauge.builder(CONSUMER_HEALTH_METRIC, consumerMonitoringService, this::consumerHealthValue)
                .description("Outbox consumer health status (0=ok/no-data, 1=warning, 2=critical)")
                .register(registry);

        FunctionCounter.builder(CONSUMER_EVENTS_METRIC, consumerMonitoringService,
                        service -> service.getMetrics().processedEventCount())
                .description("Outbox consumer events by outcome")
                .tag("outcome", "processed")
                .register(registry);

        FunctionCounter.builder(CONSUMER_EVENTS_METRIC, consumerMonitoringService,
                        service -> service.getMetrics().duplicateEventCount())
                .description("Outbox consumer events by outcome")
                .tag("outcome", "duplicate")
                .register(registry);
    }

    private double relayHealthValue(OperationOutboxRelayMonitoringService service) {
        OutboxRelayHealthStatus status = service.getHealthSummary().status();
        return switch (status) {
            case OK, NO_DATA -> 0.0;
            case WARNING -> 1.0;
            case CRITICAL -> 2.0;
        };
    }

    private double consumerHealthValue(OperationOutboxConsumerMonitoringService service) {
        OperationOutboxConsumerHealthStatus status = service.getHealthSummary().status();
        return switch (status) {
            case OK, NO_DATA -> 0.0;
            case WARNING -> 1.0;
            case CRITICAL -> 2.0;
        };
    }
}
