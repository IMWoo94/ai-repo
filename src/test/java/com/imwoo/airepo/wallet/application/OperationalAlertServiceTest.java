package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.OperationalAlert;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class OperationalAlertServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final OperationalAlertService operationalAlertService = new OperationalAlertService(
            repository,
            new OperationalAlertPolicy(15, 30),
            operationalAlert -> {
            }
    );

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
    void publishesSavedAlertToExternalPublisher() {
        List<OperationalAlert> publishedAlerts = new CopyOnWriteArrayList<>();
        OperationalAlertService service = new OperationalAlertService(
                repository,
                new OperationalAlertPolicy(15, 30),
                publishedAlerts::add
        );

        service.publishHealthAlert(
                "OUTBOX_RELAY",
                "WARNING",
                Instant.parse("2026-05-01T00:00:00Z"),
                List.of("warning relay failure rate")
        );

        assertThat(publishedAlerts)
                .singleElement()
                .satisfies(alert -> assertThat(alert.alertId()).isEqualTo("operational-alert-001"));
    }

    @Test
    void externalPublisherFailureDoesNotFailAlertRecord() {
        OperationalAlertService service = new OperationalAlertService(
                repository,
                new OperationalAlertPolicy(15, 30),
                operationalAlert -> {
                    throw new IllegalStateException("slack down");
                }
        );

        service.publishHealthAlert(
                "OUTBOX_RELAY",
                "WARNING",
                Instant.parse("2026-05-01T00:00:00Z"),
                List.of("warning relay failure rate")
        );

        assertThat(service.findRecentAlerts(10))
                .singleElement()
                .satisfies(alert -> assertThat(alert.alertId()).isEqualTo("operational-alert-001"));
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
    void suppressesDuplicateAlertsWithinWindow() {
        operationalAlertService.publishHealthAlert(
                "OUTBOX_RELAY",
                "WARNING",
                Instant.parse("2026-05-01T00:00:00Z"),
                List.of("warning relay failure rate")
        );
        operationalAlertService.publishHealthAlert(
                "OUTBOX_RELAY",
                "WARNING",
                Instant.parse("2026-05-01T00:14:59Z"),
                List.of("warning relay failure rate")
        );
        operationalAlertService.publishHealthAlert(
                "OUTBOX_RELAY",
                "WARNING",
                Instant.parse("2026-05-01T00:15:01Z"),
                List.of("warning relay failure rate")
        );

        assertThat(operationalAlertService.findRecentAlerts(10))
                .extracting("alertId")
                .containsExactly("operational-alert-002", "operational-alert-001");
    }

    @Test
    void suppressesConcurrentDuplicateAlertsWithinSingleApplicationInstance() throws Exception {
        int requestCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);
        List<Exception> failures = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(requestCount)) {
            for (int index = 0; index < requestCount; index++) {
                executor.submit(() -> {
                    try {
                        startLatch.await(1, TimeUnit.SECONDS);
                        operationalAlertService.publishHealthAlert(
                                "OUTBOX_RELAY",
                                "WARNING",
                                Instant.parse("2026-05-01T00:00:00Z"),
                                List.of("warning relay failure rate")
                        );
                    } catch (Exception exception) {
                        synchronized (failures) {
                            failures.add(exception);
                        }
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(doneLatch.await(3, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(failures).isEmpty();
        assertThat(operationalAlertService.findRecentAlerts(10))
                .singleElement()
                .satisfies(alert -> assertThat(alert.alertId()).isEqualTo("operational-alert-001"));
    }

    @Test
    void doesNotSuppressEarlierAlertWhenNewerAlertAlreadyExists() {
        operationalAlertService.publishHealthAlert(
                "OUTBOX_RELAY",
                "WARNING",
                Instant.parse("2026-05-01T00:20:00Z"),
                List.of("warning relay failure rate")
        );
        operationalAlertService.publishHealthAlert(
                "OUTBOX_RELAY",
                "WARNING",
                Instant.parse("2026-05-01T00:00:00Z"),
                List.of("warning relay failure rate")
        );

        assertThat(operationalAlertService.findRecentAlerts(10))
                .extracting("alertId")
                .containsExactly("operational-alert-001", "operational-alert-002");
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
