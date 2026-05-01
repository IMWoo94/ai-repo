package com.imwoo.airepo.wallet.application;

import com.imwoo.airepo.wallet.domain.OperationOutboxEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxRequeueAudit;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OperationOutboxRelayService {

    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(30);
    private static final Duration PROCESSING_LEASE = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 3;

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

    public List<OperationOutboxEvent> getManualReviewEvents(int limit) {
        if (limit <= 0) {
            throw new InvalidWalletOperationException("limit must be positive");
        }
        return operationOutboxRelayRepository.findManualReviewOutboxEvents(limit);
    }

    public List<OperationOutboxRequeueAudit> getRequeueAudits(String outboxEventId) {
        validateOutboxEventId(outboxEventId);
        return operationOutboxRelayRepository.findOutboxRequeueAudits(outboxEventId);
    }

    public List<OperationOutboxEvent> claimReadyEvents(int limit) {
        if (limit <= 0) {
            throw new InvalidWalletOperationException("limit must be positive");
        }
        Instant now = Instant.now(clock);
        return operationOutboxRelayRepository.claimReadyOutboxEvents(limit, now, now.plus(PROCESSING_LEASE));
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
        operationOutboxRelayRepository.markOutboxEventFailed(
                outboxEventId,
                lastError,
                Instant.now(clock).plus(RETRY_BACKOFF),
                MAX_ATTEMPTS
        );
    }

    public void requeueManualReviewEvent(String outboxEventId, String operator, String reason) {
        validateOutboxEventId(outboxEventId);
        validateRequired("operator", operator);
        validateRequired("reason", reason);
        operationOutboxRelayRepository.requeueManualReviewOutboxEvent(
                outboxEventId,
                Instant.now(clock),
                operator,
                reason
        );
    }

    private void validateOutboxEventId(String outboxEventId) {
        if (outboxEventId == null || outboxEventId.isBlank()) {
            throw new InvalidWalletOperationException("outboxEventId must not be blank");
        }
    }

    private void validateRequired(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidWalletOperationException(fieldName + " must not be blank");
        }
    }
}
