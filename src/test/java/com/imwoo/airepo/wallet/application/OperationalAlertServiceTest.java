package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class OperationalAlertServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final OperationalAlertService operationalAlertService = new OperationalAlertService(repository);

    @Test
    void publishesWarningAndCriticalHealthAlerts() {
        operationalAlertService.publishHealthAlert(
                "OUTBOX_RELAY",
                "WARNING",
                Instant.parse("2026-05-01T00:00:00Z"),
                List.of("warning relay failure rate")
        );
        operationalAlertService.publishHealthAlert(
                "OUTBOX_CONSUMER",
                "CRITICAL",
                Instant.parse("2026-05-01T00:01:00Z"),
                List.of("critical consumer duplicate delivery rate in health window")
        );

        assertThat(operationalAlertService.findRecentAlerts(10))
                .hasSize(2)
                .first()
                .satisfies(alert -> {
                    assertThat(alert.alertId()).isEqualTo("operational-alert-002");
                    assertThat(alert.source()).isEqualTo("OUTBOX_CONSUMER");
                    assertThat(alert.severity().name()).isEqualTo("CRITICAL");
                    assertThat(alert.reasons()).containsExactly(
                            "critical consumer duplicate delivery rate in health window"
                    );
                });
    }

    @Test
    void skipsOkNoDataAndEmptyReasonAlerts() {
        operationalAlertService.publishHealthAlert(
                "OUTBOX_RELAY",
                "OK",
                Instant.parse("2026-05-01T00:00:00Z"),
                List.of()
        );
        operationalAlertService.publishHealthAlert(
                "OUTBOX_CONSUMER",
                "NO_DATA",
                Instant.parse("2026-05-01T00:01:00Z"),
                List.of("no consumer event data in health window")
        );
        operationalAlertService.publishHealthAlert(
                "OUTBOX_CONSUMER",
                "CRITICAL",
                Instant.parse("2026-05-01T00:02:00Z"),
                List.of()
        );

        assertThat(operationalAlertService.findRecentAlerts(10)).isEmpty();
    }

    @Test
    void rejectsInvalidRecentAlertLimit() {
        assertThatThrownBy(() -> operationalAlertService.findRecentAlerts(0))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("limit must be between 1 and 100");
        assertThatThrownBy(() -> operationalAlertService.findRecentAlerts(101))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("limit must be between 1 and 100");
    }
}
