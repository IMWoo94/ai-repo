package com.imwoo.airepo.wallet.application;

/**
 * Application port that records relay run outcomes into a metrics sink.
 *
 * <p>The relay run store keeps only a bounded recent sample, so cumulative counters cannot be
 * derived from it after the fact. This recorder captures each relay outcome at the moment it is
 * recorded, allowing an infrastructure adapter (Micrometer) to expose monotonically increasing
 * counters. The default no-op implementation keeps the monitoring service usable without a metrics
 * backend.
 */
public interface OutboxRelayMetricsRecorder {

    OutboxRelayMetricsRecorder NO_OP = new OutboxRelayMetricsRecorder() {
    };

    default void recordRelaySuccess(int publishedCount, int failedCount) {
    }

    default void recordRelayFailure() {
    }
}
