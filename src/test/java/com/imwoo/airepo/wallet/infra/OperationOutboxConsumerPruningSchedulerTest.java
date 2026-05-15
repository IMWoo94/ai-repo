package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.imwoo.airepo.wallet.application.OperationOutboxConsumerPruningPolicy;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerPruningResult;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerPruningService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OperationOutboxConsumerPruningSchedulerTest {

    @Test
    void runsPruningWithConfiguredRetention() {
        OperationOutboxConsumerPruningService pruningService = mock(OperationOutboxConsumerPruningService.class);
        OperationOutboxConsumerPruningPolicy pruningPolicy = new OperationOutboxConsumerPruningPolicy(30, 45, 7);
        OperationOutboxConsumerPruningResult expectedResult = new OperationOutboxConsumerPruningResult(
                Instant.parse("2026-05-02T00:00:00Z"),
                Instant.parse("2026-04-02T00:00:00Z"),
                Instant.parse("2026-03-18T00:00:00Z"),
                Instant.parse("2026-04-25T00:00:00Z"),
                2,
                3,
                4
        );
        when(pruningService.prune(Duration.ofDays(30), Duration.ofDays(45), Duration.ofDays(7)))
                .thenReturn(expectedResult);
        OperationOutboxConsumerPruningScheduler scheduler = new OperationOutboxConsumerPruningScheduler(
                pruningService,
                pruningPolicy
        );

        OperationOutboxConsumerPruningResult result = scheduler.runOnce();

        assertThat(result).isEqualTo(expectedResult);
        verify(pruningService).prune(Duration.ofDays(30), Duration.ofDays(45), Duration.ofDays(7));
    }
}
