package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRunStatus;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OperationOutboxRelayMonitoringServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final OperationOutboxRelayMonitoringService monitoringService = new OperationOutboxRelayMonitoringService(
            repository,
            new OutboxRelayHealthPolicy(5, 2, 3, 50, 15),
            Clock.fixed(Instant.parse("2026-05-01T00:20:00Z"), ZoneOffset.UTC)
    );

    @Test
    void recordsSuccessfulRelayRun() {
        monitoringService.recordSuccess(
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:01Z"),
                10,
                new OperationOutboxPublishBatchResult(3, 2, 1)
        );

        assertThat(monitoringService.getRecentRuns(10))
                .singleElement()
                .satisfies(relayRun -> {
                    assertThat(relayRun.relayRunId()).isEqualTo("outbox-relay-run-001");
                    assertThat(relayRun.status()).isEqualTo(OperationOutboxRelayRunStatus.SUCCESS);
                    assertThat(relayRun.batchSize()).isEqualTo(10);
                    assertThat(relayRun.claimedCount()).isEqualTo(3);
                    assertThat(relayRun.publishedCount()).isEqualTo(2);
                    assertThat(relayRun.failedCount()).isEqualTo(1);
                    assertThat(relayRun.errorMessage()).isNull();
                });
    }

    @Test
    void recordsFailedRelayRun() {
        monitoringService.recordFailure(
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:01Z"),
                10,
                "publisher down"
        );

        assertThat(monitoringService.getRecentRuns(10))
                .singleElement()
                .satisfies(relayRun -> {
                    assertThat(relayRun.relayRunId()).isEqualTo("outbox-relay-run-001");
                    assertThat(relayRun.status()).isEqualTo(OperationOutboxRelayRunStatus.FAILED);
                    assertThat(relayRun.errorMessage()).isEqualTo("publisher down");
                });
    }

    @Test
    void normalizesLongRelayFailureMessage() {
        monitoringService.recordFailure(
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:01Z"),
                10,
                "x".repeat(300)
        );

        assertThat(monitoringService.getRecentRuns(10))
                .singleElement()
                .satisfies(relayRun -> assertThat(relayRun.errorMessage()).hasSize(255));
    }

    @Test
    void rejectsInvalidRecentRunLimit() {
        assertThatThrownBy(() -> monitoringService.getRecentRuns(0))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("limit must be positive");
    }

    @Test
    void returnsNoDataHealthSummaryWhenRelayRunDoesNotExist() {
        OutboxRelayHealthSummary summary = monitoringService.getHealthSummary();

        assertThat(summary.status()).isEqualTo(OutboxRelayHealthStatus.NO_DATA);
        assertThat(summary.totalRunCount()).isZero();
        assertThat(summary.alertReasons()).containsExactly("no relay run data");
    }

    @Test
    void returnsOkHealthSummaryForHealthyRecentRelayRuns() {
        monitoringService.recordSuccess(
                Instant.parse("2026-05-01T00:18:00Z"),
                Instant.parse("2026-05-01T00:18:01Z"),
                10,
                new OperationOutboxPublishBatchResult(1, 1, 0)
        );

        OutboxRelayHealthSummary summary = monitoringService.getHealthSummary();

        assertThat(summary.status()).isEqualTo(OutboxRelayHealthStatus.OK);
        assertThat(summary.totalRunCount()).isEqualTo(1);
        assertThat(summary.successCount()).isEqualTo(1);
        assertThat(summary.failedCount()).isZero();
        assertThat(summary.failureRate()).isZero();
        assertThat(summary.consecutiveFailureCount()).isZero();
        assertThat(summary.lastSuccessAt()).isEqualTo(Instant.parse("2026-05-01T00:18:01Z"));
        assertThat(summary.alertReasons()).isEmpty();
    }

    @Test
    void returnsWarningForConsecutiveFailures() {
        monitoringService.recordSuccess(
                Instant.parse("2026-05-01T00:18:00Z"),
                Instant.parse("2026-05-01T00:18:01Z"),
                10,
                new OperationOutboxPublishBatchResult(1, 1, 0)
        );
        monitoringService.recordFailure(
                Instant.parse("2026-05-01T00:19:00Z"),
                Instant.parse("2026-05-01T00:19:01Z"),
                10,
                "publisher down"
        );
        monitoringService.recordFailure(
                Instant.parse("2026-05-01T00:19:30Z"),
                Instant.parse("2026-05-01T00:19:31Z"),
                10,
                "publisher down"
        );

        OutboxRelayHealthSummary summary = monitoringService.getHealthSummary();

        assertThat(summary.status()).isEqualTo(OutboxRelayHealthStatus.WARNING);
        assertThat(summary.consecutiveFailureCount()).isEqualTo(2);
        assertThat(summary.failureRate()).isEqualTo(2.0 / 3.0);
        assertThat(summary.alertReasons()).contains(
                "warning consecutive relay failures",
                "warning relay failure rate"
        );
    }

    @Test
    void returnsCriticalForCriticalConsecutiveFailures() {
        monitoringService.recordSuccess(
                Instant.parse("2026-05-01T00:18:00Z"),
                Instant.parse("2026-05-01T00:18:01Z"),
                10,
                new OperationOutboxPublishBatchResult(1, 1, 0)
        );
        monitoringService.recordFailure(
                Instant.parse("2026-05-01T00:19:00Z"),
                Instant.parse("2026-05-01T00:19:01Z"),
                10,
                "publisher down"
        );
        monitoringService.recordFailure(
                Instant.parse("2026-05-01T00:19:10Z"),
                Instant.parse("2026-05-01T00:19:11Z"),
                10,
                "publisher down"
        );
        monitoringService.recordFailure(
                Instant.parse("2026-05-01T00:19:20Z"),
                Instant.parse("2026-05-01T00:19:21Z"),
                10,
                "publisher down"
        );

        OutboxRelayHealthSummary summary = monitoringService.getHealthSummary();

        assertThat(summary.status()).isEqualTo(OutboxRelayHealthStatus.CRITICAL);
        assertThat(summary.consecutiveFailureCount()).isEqualTo(3);
        assertThat(summary.alertReasons()).contains("critical consecutive relay failures");
    }

    @Test
    void returnsCriticalWhenLastSuccessIsStale() {
        monitoringService.recordSuccess(
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:01Z"),
                10,
                new OperationOutboxPublishBatchResult(1, 1, 0)
        );

        OutboxRelayHealthSummary summary = monitoringService.getHealthSummary();

        assertThat(summary.status()).isEqualTo(OutboxRelayHealthStatus.CRITICAL);
        assertThat(summary.alertReasons()).contains("last successful relay run is stale");
    }
}
