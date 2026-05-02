package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.imwoo.airepo.wallet.application.OperationOutboxPublishBatchResult;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayMonitoringService;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OperationOutboxRelaySchedulerTest {

    @Test
    void runsRelayPublishBatchWithConfiguredBatchSize() {
        OperationOutboxRelayService relayService = mock(OperationOutboxRelayService.class);
        OperationOutboxRelayMonitoringService monitoringService = mock(OperationOutboxRelayMonitoringService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
        OperationOutboxPublishBatchResult expectedResult = new OperationOutboxPublishBatchResult(3, 2, 1);
        when(relayService.publishReadyEvents(3)).thenReturn(expectedResult);
        OperationOutboxRelayScheduler scheduler = new OperationOutboxRelayScheduler(
                relayService,
                monitoringService,
                clock,
                3
        );

        OperationOutboxPublishBatchResult result = scheduler.runOnce();

        assertThat(result).isEqualTo(expectedResult);
        verify(relayService).publishReadyEvents(3);
        verify(monitoringService).recordSuccess(
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:00Z"),
                3,
                expectedResult
        );
    }

    @Test
    void recordsFailedRunAndRethrowsException() {
        OperationOutboxRelayService relayService = mock(OperationOutboxRelayService.class);
        OperationOutboxRelayMonitoringService monitoringService = mock(OperationOutboxRelayMonitoringService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);
        when(relayService.publishReadyEvents(3)).thenThrow(new IllegalStateException("publisher down"));
        OperationOutboxRelayScheduler scheduler = new OperationOutboxRelayScheduler(
                relayService,
                monitoringService,
                clock,
                3
        );

        assertThatThrownBy(scheduler::runOnce)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("publisher down");
        verify(monitoringService).recordFailure(
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-01T00:00:00Z"),
                3,
                "publisher down"
        );
    }

    @Test
    void rejectsInvalidBatchSize() {
        OperationOutboxRelayService relayService = mock(OperationOutboxRelayService.class);
        OperationOutboxRelayMonitoringService monitoringService = mock(OperationOutboxRelayMonitoringService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC);

        assertThatThrownBy(() -> new OperationOutboxRelayScheduler(relayService, monitoringService, clock, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox relay scheduler batch-size must be positive");
        verifyNoInteractions(relayService, monitoringService);
    }
}
