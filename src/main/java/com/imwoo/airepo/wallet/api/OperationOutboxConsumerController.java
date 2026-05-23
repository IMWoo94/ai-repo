package com.imwoo.airepo.wallet.api;

import com.imwoo.airepo.wallet.application.InvalidWalletOperationException;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerEnvelope;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerResult;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerService;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/broker/outbox-events")
public class OperationOutboxConsumerController {

    private final OperationOutboxConsumerService operationOutboxConsumerService;

    public OperationOutboxConsumerController(OperationOutboxConsumerService operationOutboxConsumerService) {
        this.operationOutboxConsumerService = operationOutboxConsumerService;
    }

    @PostMapping
    public ResponseEntity<OperationOutboxConsumerResult> consume(
            @RequestHeader("X-Outbox-Event-Id") String outboxEventId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Event-Schema-Version") String schemaVersion,
            @RequestHeader("X-Event-Type") String eventType,
            @RequestBody OperationOutboxConsumerRequest request
    ) {
        assertHeaderMatchesBody("X-Outbox-Event-Id", outboxEventId, request.outboxEventId());
        assertHeaderMatchesBody("X-Idempotency-Key", idempotencyKey, request.idempotencyKey());
        assertHeaderMatchesBody("X-Event-Type", eventType, request.eventType());
        if (schemaVersion(schemaVersion) != request.schemaVersion()) {
            throw new InvalidWalletOperationException("X-Event-Schema-Version must match body schemaVersion");
        }
        return ResponseEntity.accepted().body(operationOutboxConsumerService.consume(new OperationOutboxConsumerEnvelope(
                request.schemaVersion(),
                request.idempotencyKey(),
                request.outboxEventId(),
                request.operationId(),
                request.eventType(),
                request.aggregateType(),
                request.aggregateId(),
                Instant.parse(request.occurredAt()),
                String.valueOf(request.payload())
        )));
    }

    private void assertHeaderMatchesBody(String headerName, String headerValue, String bodyValue) {
        if (!headerValue.equals(bodyValue)) {
            throw new InvalidWalletOperationException(headerName + " must match body");
        }
    }

    private int schemaVersion(String schemaVersion) {
        try {
            return Integer.parseInt(schemaVersion);
        } catch (NumberFormatException exception) {
            throw new InvalidWalletOperationException("X-Event-Schema-Version must be an integer");
        }
    }

    public record OperationOutboxConsumerRequest(
            int schemaVersion,
            String idempotencyKey,
            String outboxEventId,
            String operationId,
            String eventType,
            String aggregateType,
            String aggregateId,
            String occurredAt,
            Object payload
    ) {
    }
}
