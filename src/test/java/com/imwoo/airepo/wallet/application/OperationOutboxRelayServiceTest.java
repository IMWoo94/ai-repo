package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.Money;
import com.imwoo.airepo.wallet.domain.OperationOutboxStatus;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OperationOutboxRelayServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final InMemoryWalletCommandService commandService = new InMemoryWalletCommandService(
            Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC),
            repository
    );
    private final OperationOutboxRelayService relayService = new OperationOutboxRelayService(
            Clock.fixed(Instant.parse("2026-05-01T00:01:00Z"), ZoneOffset.UTC),
            repository
    );

    @Test
    void returnsPendingEventsWithLimit() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        commandService.transfer(
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("1000"), "transfer-001", "테스트 송금")
        );

        assertThat(relayService.getPendingEvents(1))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.outboxEventId()).isEqualTo("outbox-001");
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PENDING);
                    assertThat(outboxEvent.attemptCount()).isZero();
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.publishedAt()).isNull();
                    assertThat(outboxEvent.lastError()).isNull();
                });
    }

    @Test
    void claimsPendingEventsAsProcessingWithLimit() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        commandService.transfer(
                "wallet-001",
                new WalletTransferCommand("wallet-002", money("1000"), "transfer-001", "테스트 송금")
        );

        assertThat(relayService.claimReadyEvents(1))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.outboxEventId()).isEqualTo("outbox-001");
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING);
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.publishedAt()).isNull();
                    assertThat(outboxEvent.lastError()).isNull();
                });
        assertThat(relayService.getPendingEvents(10))
                .singleElement()
                .satisfies(outboxEvent -> assertThat(outboxEvent.outboxEventId()).isEqualTo("outbox-002"));
    }

    @Test
    void marksEventPublished() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));

        relayService.markPublished("outbox-001");

        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PUBLISHED);
                    assertThat(outboxEvent.publishedAt()).isEqualTo(Instant.parse("2026-05-01T00:01:00Z"));
                    assertThat(outboxEvent.attemptCount()).isZero();
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.lastError()).isNull();
                });
        assertThat(relayService.getPendingEvents(10)).isEmpty();
    }

    @Test
    void marksEventFailedWithAttemptCountAndLastError() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));

        relayService.markFailed("outbox-001", "broker unavailable");

        assertThat(repository.findOperationOutboxEvents("op-001"))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.FAILED);
                    assertThat(outboxEvent.attemptCount()).isEqualTo(1);
                    assertThat(outboxEvent.nextRetryAt()).isEqualTo(Instant.parse("2026-05-01T00:01:30Z"));
                    assertThat(outboxEvent.lastError()).isEqualTo("broker unavailable");
                    assertThat(outboxEvent.publishedAt()).isNull();
                });
        assertThat(relayService.getPendingEvents(10)).isEmpty();
    }

    @Test
    void claimsFailedEventOnlyAfterNextRetryAt() {
        commandService.charge("wallet-001", new WalletChargeCommand(money("5000"), "charge-001", "테스트 충전"));
        relayService.markFailed("outbox-001", "broker unavailable");

        assertThat(relayService.claimReadyEvents(10)).isEmpty();

        OperationOutboxRelayService retryReadyRelayService = new OperationOutboxRelayService(
                Clock.fixed(Instant.parse("2026-05-01T00:01:31Z"), ZoneOffset.UTC),
                repository
        );
        assertThat(retryReadyRelayService.claimReadyEvents(10))
                .singleElement()
                .satisfies(outboxEvent -> {
                    assertThat(outboxEvent.status()).isEqualTo(OperationOutboxStatus.PROCESSING);
                    assertThat(outboxEvent.nextRetryAt()).isNull();
                    assertThat(outboxEvent.lastError()).isNull();
                });
    }

    @Test
    void rejectsInvalidRelayInputs() {
        assertThatThrownBy(() -> relayService.getPendingEvents(0))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("limit must be positive");
        assertThatThrownBy(() -> relayService.claimReadyEvents(0))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("limit must be positive");
        assertThatThrownBy(() -> relayService.markPublished(" "))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("outboxEventId must not be blank");
        assertThatThrownBy(() -> relayService.markFailed("outbox-001", " "))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("lastError must not be blank");
    }

    private Money money(String amount) {
        return new Money(new BigDecimal(amount), "KRW");
    }
}
