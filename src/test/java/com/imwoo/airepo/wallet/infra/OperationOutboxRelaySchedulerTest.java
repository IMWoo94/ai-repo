package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.imwoo.airepo.wallet.application.OperationOutboxPublishBatchResult;
import com.imwoo.airepo.wallet.application.OperationOutboxRelayService;
import org.junit.jupiter.api.Test;

class OperationOutboxRelaySchedulerTest {

    @Test
    void runsRelayPublishBatchWithConfiguredBatchSize() {
        OperationOutboxRelayService relayService = mock(OperationOutboxRelayService.class);
        OperationOutboxPublishBatchResult expectedResult = new OperationOutboxPublishBatchResult(3, 2, 1);
        when(relayService.publishReadyEvents(3)).thenReturn(expectedResult);
        OperationOutboxRelayScheduler scheduler = new OperationOutboxRelayScheduler(relayService, 3);

        OperationOutboxPublishBatchResult result = scheduler.runOnce();

        assertThat(result).isEqualTo(expectedResult);
        verify(relayService).publishReadyEvents(3);
    }

    @Test
    void rejectsInvalidBatchSize() {
        OperationOutboxRelayService relayService = mock(OperationOutboxRelayService.class);

        assertThatThrownBy(() -> new OperationOutboxRelayScheduler(relayService, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outbox relay scheduler batch-size must be positive");
    }
}
