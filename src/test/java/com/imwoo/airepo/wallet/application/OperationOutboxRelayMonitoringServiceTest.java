package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.OperationOutboxRelayRunStatus;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OperationOutboxRelayMonitoringServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final OperationOutboxRelayMonitoringService monitoringService = new OperationOutboxRelayMonitoringService(
            repository
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
}
