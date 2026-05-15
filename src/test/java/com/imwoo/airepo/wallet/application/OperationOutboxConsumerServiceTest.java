package com.imwoo.airepo.wallet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imwoo.airepo.wallet.infra.InMemoryWalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OperationOutboxConsumerServiceTest {

    private final InMemoryWalletRepository repository = new InMemoryWalletRepository();
    private final OperationOutboxConsumerService service = new OperationOutboxConsumerService(
            repository,
            repository,
            repository,
            Clock.fixed(Instant.parse("2026-05-01T00:05:00Z"), ZoneOffset.UTC)
    );

    @Test
    void consumesEventOnceAndSkipsDuplicateSideEffect() {
        OperationOutboxConsumerResult firstResult = service.consume(envelope());
        OperationOutboxConsumerResult duplicateResult = service.consume(envelope());

        assertThat(firstResult.processed()).isTrue();
        assertThat(duplicateResult.processed()).isFalse();
        assertThat(repository.findProcessedEvent("outbox-001"))
                .hasValueSatisfying(processedEvent -> assertThat(processedEvent.processedAt())
                        .isEqualTo(Instant.parse("2026-05-01T00:05:00Z")));
        assertThat(repository.findConsumerReceipt("outbox-001"))
                .hasValueSatisfying(receipt -> {
                    assertThat(receipt.operationId()).isEqualTo("op-001");
                    assertThat(receipt.eventType()).isEqualTo("CHARGE_COMPLETED");
                    assertThat(receipt.receivedAt()).isEqualTo(Instant.parse("2026-05-01T00:05:00Z"));
                });
        assertThat(repository.getConsumerWindowMetrics(
                Instant.parse("2026-05-01T00:05:00Z"),
                Instant.parse("2026-05-01T00:06:00Z")
        ))
                .satisfies(metrics -> {
                    assertThat(metrics.processedDeliveryCount()).isEqualTo(1);
                    assertThat(metrics.duplicateDeliveryCount()).isEqualTo(1);
                });
    }

    @Test
    void rejectsUnsupportedSchemaVersionWithoutRecordingSideEffect() {
        assertThatThrownBy(() -> service.consume(new OperationOutboxConsumerEnvelope(
                2,
                "outbox-001",
                "outbox-001",
                "op-001",
                "CHARGE_COMPLETED",
                "WALLET_OPERATION",
                "op-001",
                Instant.parse("2026-05-01T00:00:00Z"),
                "{\"operationId\":\"op-001\"}"
        )))
                .isInstanceOf(InvalidWalletOperationException.class)
                .hasMessage("unsupported event schema version: 2");

        assertThat(repository.findProcessedEvent("outbox-001")).isEmpty();
        assertThat(repository.findConsumerReceipt("outbox-001")).isEmpty();
    }

    private OperationOutboxConsumerEnvelope envelope() {
        return new OperationOutboxConsumerEnvelope(
                1,
                "outbox-001",
                "outbox-001",
                "op-001",
                "CHARGE_COMPLETED",
                "WALLET_OPERATION",
                "op-001",
                Instant.parse("2026-05-01T00:00:00Z"),
                "{\"operationId\":\"op-001\"}"
        );
    }
}
