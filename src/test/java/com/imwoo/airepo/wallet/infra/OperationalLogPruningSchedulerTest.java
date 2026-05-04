package com.imwoo.airepo.wallet.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.imwoo.airepo.wallet.application.OperationalLogPruningPolicy;
import com.imwoo.airepo.wallet.application.OperationalLogPruningResult;
import com.imwoo.airepo.wallet.application.OperationalLogPruningService;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OperationalLogPruningSchedulerTest {

    @Test
    void runsPruningWithConfiguredRetention() {
        OperationalLogPruningService pruningService = mock(OperationalLogPruningService.class);
        OperationalLogPruningPolicy pruningPolicy = new OperationalLogPruningPolicy(30, 180);
        OperationalLogPruningResult expectedResult = new OperationalLogPruningResult(
                Instant.parse("2026-05-02T00:00:00Z"),
                Instant.parse("2026-04-02T00:00:00Z"),
                Instant.parse("2025-11-03T00:00:00Z"),
                2,
                3
        );
        when(pruningService.prune(Duration.ofDays(30), Duration.ofDays(180))).thenReturn(expectedResult);
        OperationalLogPruningScheduler scheduler = new OperationalLogPruningScheduler(pruningService, pruningPolicy);

        OperationalLogPruningResult result = scheduler.runOnce();

        assertThat(result).isEqualTo(expectedResult);
        verify(pruningService).prune(Duration.ofDays(30), Duration.ofDays(180));
    }
}
