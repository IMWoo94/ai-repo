package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OperationOutboxConsumerPruningServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final OperationOutboxConsumerPruningService pruningService = new OperationOutboxConsumerPruningService(
            repository,
            Clock.fixed(Instant.parse("2026-05-02T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void prunesConsumerProcessedEventsAndReceiptsOlderThanRetentionCutoff() {
        repository.recordProcessedEvent(
                "outbox-001",
                "outbox-001",
                "CHARGE_COMPLETED",
                Instant.parse("2026-04-30T23:59:59Z")
        );
        repository.recordProcessedEvent(
                "outbox-002",
                "outbox-002",
                "CHARGE_COMPLETED",
                Instant.parse("2026-05-01T00:00:00Z")
        );
        repository.saveConsumerReceipt(receipt("outbox-001", "2026-04-30T23:59:59Z"));
        repository.saveConsumerReceipt(receipt("outbox-002", "2026-05-01T00:00:00Z"));

        OperationOutboxConsumerPruningResult result = pruningService.prune(Duration.ofDays(1), Duration.ofDays(1));

        assertThat(result.prunedAt()).isEqualTo(Instant.parse("2026-05-02T00:00:00Z"));
        assertThat(result.processedEventCutoff()).isEqualTo(Instant.parse("2026-05-01T00:00:00Z"));
        assertThat(result.receiptCutoff()).isEqualTo(Instant.parse("2026-05-01T00:00:00Z"));
        assertThat(result.deletedProcessedEventCount()).isEqualTo(1);
        assertThat(result.deletedReceiptCount()).isEqualTo(1);
        assertThat(repository.findProcessedEvent("outbox-001")).isEmpty();
        assertThat(repository.findProcessedEvent("outbox-002")).isPresent();
        assertThat(repository.findConsumerReceipt("outbox-001")).isEmpty();
        assertThat(repository.findConsumerReceipt("outbox-002")).isPresent();
    }

    @Test
    void rejectsInvalidRetention() {
        assertThatThrownBy(() -> pruningService.prune(Duration.ZERO, Duration.ofDays(1)))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("processedEventRetention must be positive");
        assertThatThrownBy(() -> pruningService.prune(Duration.ofDays(1), Duration.ZERO))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("receiptRetention must be positive");
    }

    private OperationOutboxConsumerReceipt receipt(String idempotencyKey, String receivedAt) {
        return new OperationOutboxConsumerReceipt(
                idempotencyKey,
                idempotencyKey,
                "op-" + idempotencyKey.substring(idempotencyKey.length() - 3),
                "CHARGE_COMPLETED",
                "WALLET_OPERATION",
                "op-" + idempotencyKey.substring(idempotencyKey.length() - 3),
                Instant.parse(receivedAt)
        );
    }
}
