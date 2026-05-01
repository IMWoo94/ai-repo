package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OperationOutboxRelayService {

    private final Clock clock;
    private final OperationOutboxRelayRepository operationOutboxRelayRepository;

    public OperationOutboxRelayService(
            Clock clock,
            OperationOutboxRelayRepository operationOutboxRelayRepository
    ) {
        this.clock = clock;
        this.operationOutboxRelayRepository = operationOutboxRelayRepository;
    }

    public List<OperationOutboxEvent> getPendingEvents(int limit) {
        if (limit <= 0) {
            throw new InvalidWalletOperationException("limit must be positive");
        }
        return operationOutboxRelayRepository.findPendingOutboxEvents(limit);
    }

    public void markPublished(String outboxEventId) {
        validateOutboxEventId(outboxEventId);
        operationOutboxRelayRepository.markOutboxEventPublished(outboxEventId, Instant.now(clock));
    }

    public void markFailed(String outboxEventId, String lastError) {
        validateOutboxEventId(outboxEventId);
        if (lastError == null || lastError.isBlank()) {
            throw new InvalidWalletOperationException("lastError must not be blank");
        }
        operationOutboxRelayRepository.markOutboxEventFailed(outboxEventId, lastError);
    }

    private void validateOutboxEventId(String outboxEventId) {
        if (outboxEventId == null || outboxEventId.isBlank()) {
            throw new InvalidWalletOperationException("outboxEventId must not be blank");
        }
    }
}
