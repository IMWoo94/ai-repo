package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationOutboxConsumerService {

    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final OperationOutboxConsumerIdempotencyRepository idempotencyRepository;
    private final OperationOutboxConsumerReceiptRepository receiptRepository;
    private final Clock clock;

    public OperationOutboxConsumerService(
            OperationOutboxConsumerIdempotencyRepository idempotencyRepository,
            OperationOutboxConsumerReceiptRepository receiptRepository,
            Clock clock
    ) {
        this.idempotencyRepository = idempotencyRepository;
        this.receiptRepository = receiptRepository;
        this.clock = clock;
    }

    @Transactional
    public OperationOutboxConsumerResult consume(OperationOutboxConsumerEnvelope envelope) {
        if (envelope.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            throw new InvalidWalletOperationException("unsupported event schema version: " + envelope.schemaVersion());
        }
        if (!envelope.idempotencyKey().equals(envelope.outboxEventId())) {
            throw new InvalidWalletOperationException("idempotencyKey must match outboxEventId");
        }

        Instant processedAt = Instant.now(clock);
        boolean recorded = idempotencyRepository.recordProcessedEvent(
                envelope.idempotencyKey(),
                envelope.outboxEventId(),
                envelope.eventType(),
                processedAt
        );
        if (!recorded) {
            return result(envelope, false);
        }

        receiptRepository.saveConsumerReceipt(new OperationOutboxConsumerReceipt(
                envelope.idempotencyKey(),
                envelope.outboxEventId(),
                envelope.operationId(),
                envelope.eventType(),
                envelope.aggregateType(),
                envelope.aggregateId(),
                processedAt
        ));
        return result(envelope, true);
    }

    private OperationOutboxConsumerResult result(OperationOutboxConsumerEnvelope envelope, boolean processed) {
        return new OperationOutboxConsumerResult(
                envelope.idempotencyKey(),
                envelope.outboxEventId(),
                envelope.eventType(),
                processed
        );
    }
}
