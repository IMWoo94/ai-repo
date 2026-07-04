package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OutboxRelayMetricsRecorder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer-backed adapter for {@link OutboxRelayMetricsRecorder}. Holds monotonic relay counters
 * so they survive the relay run store's bounded retention. It has no dependency on the monitoring
 * services, which keeps it free of the wiring cycle that gauges (which read those services) create.
 */
@Component
public class OutboxRelayMetricsRegistry implements OutboxRelayMetricsRecorder {

    static final String RELAY_RUNS_METRIC = "ai.repo.outbox.relay.runs";
    static final String RELAY_PUBLISHED_METRIC = "ai.repo.outbox.relay.published.events";

    private final Counter successRunCounter;
    private final Counter failureRunCounter;
    private final Counter publishedEventCounter;
    private final Counter failedPublishEventCounter;

    public OutboxRelayMetricsRegistry(MeterRegistry meterRegistry) {
        this.successRunCounter = Counter.builder(RELAY_RUNS_METRIC)
                .description("Outbox relay runs by result")
                .tag("result", "success")
                .register(meterRegistry);
        this.failureRunCounter = Counter.builder(RELAY_RUNS_METRIC)
                .description("Outbox relay runs by result")
                .tag("result", "failure")
                .register(meterRegistry);
        this.publishedEventCounter = Counter.builder(RELAY_PUBLISHED_METRIC)
                .description("Outbox events published by the relay by outcome")
                .tag("outcome", "published")
                .register(meterRegistry);
        this.failedPublishEventCounter = Counter.builder(RELAY_PUBLISHED_METRIC)
                .description("Outbox events published by the relay by outcome")
                .tag("outcome", "failed")
                .register(meterRegistry);
    }

    @Override
    public void recordRelaySuccess(int publishedCount, int failedCount) {
        successRunCounter.increment();
        if (publishedCount > 0) {
            publishedEventCounter.increment(publishedCount);
        }
        if (failedCount > 0) {
            failedPublishEventCounter.increment(failedCount);
        }
    }

    @Override
    public void recordRelayFailure() {
        failureRunCounter.increment();
    }
}
