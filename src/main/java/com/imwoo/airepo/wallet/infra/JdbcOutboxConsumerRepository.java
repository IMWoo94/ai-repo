package com.imwoo.airepo.wallet.infra;

import com.imwoo.airepo.wallet.application.OperationOutboxConsumerDeliveryMetricRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerIdempotencyRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerMetrics;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerMonitoringRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerPruningRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerReceiptRepository;
import com.imwoo.airepo.wallet.application.OperationOutboxConsumerWindowMetrics;
import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerProcessedEvent;
import com.imwoo.airepo.wallet.domain.OperationOutboxConsumerReceipt;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JDBC adapter for the outbox consumer bounded context: idempotency dedupe, receipt persistence,
 * delivery-metric time buckets, monitoring aggregates, and retention pruning.
 */
final class JdbcOutboxConsumerRepository implements
        OperationOutboxConsumerIdempotencyRepository,
        OperationOutboxConsumerReceiptRepository,
        OperationOutboxConsumerDeliveryMetricRepository,
        OperationOutboxConsumerMonitoringRepository,
        OperationOutboxConsumerPruningRepository {

    private final WalletJdbcSupport support;
    private final JdbcTemplate jdbcTemplate;

    JdbcOutboxConsumerRepository(WalletJdbcSupport support) {
        this.support = support;
        this.jdbcTemplate = support.jdbc();
    }

    @Override
    public boolean recordProcessedEvent(
            String idempotencyKey,
            String outboxEventId,
            String eventType,
            Instant processedAt
    ) {
        if (support.isPostgresDatabase()) {
            Boolean inserted = jdbcTemplate.queryForObject(
                    """
                            insert into operation_outbox_consumer_processed_events (
                                idempotency_key, outbox_event_id, event_type, processed_at
                            )
                            values (?, ?, ?, ?)
                            on conflict (idempotency_key) do update
                            set duplicate_count =
                                operation_outbox_consumer_processed_events.duplicate_count + 1
                            returning (xmax = 0) as inserted
                            """,
                    Boolean.class,
                    idempotencyKey,
                    outboxEventId,
                    eventType,
                    support.timestamp(processedAt)
            );
            return Boolean.TRUE.equals(inserted);
        }
        int duplicateRows = jdbcTemplate.update(
                """
                        update operation_outbox_consumer_processed_events
                        set duplicate_count = duplicate_count + 1
                        where idempotency_key = ?
                        """,
                idempotencyKey
        );
        if (duplicateRows == 1) {
            return false;
        }
        try {
            return jdbcTemplate.update(
                    """
                            insert into operation_outbox_consumer_processed_events (
                                idempotency_key, outbox_event_id, event_type, processed_at
                            )
                            values (?, ?, ?, ?)
                            """,
                    idempotencyKey,
                    outboxEventId,
                    eventType,
                    support.timestamp(processedAt)
            ) == 1;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    @Override
    public Optional<OperationOutboxConsumerProcessedEvent> findProcessedEvent(String idempotencyKey) {
        return support.queryOptional(
                """
                        select idempotency_key, outbox_event_id, event_type, processed_at, duplicate_count
                        from operation_outbox_consumer_processed_events
                        where idempotency_key = ?
                        """,
                support.operationOutboxConsumerProcessedEventMapper(),
                idempotencyKey
        );
    }

    @Override
    public void saveConsumerReceipt(OperationOutboxConsumerReceipt receipt) {
        jdbcTemplate.update(
                """
                        insert into operation_outbox_consumer_receipts (
                            idempotency_key, outbox_event_id, operation_id, event_type,
                            aggregate_type, aggregate_id, received_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                receipt.idempotencyKey(),
                receipt.outboxEventId(),
                receipt.operationId(),
                receipt.eventType(),
                receipt.aggregateType(),
                receipt.aggregateId(),
                support.timestamp(receipt.receivedAt())
        );
    }

    @Override
    public Optional<OperationOutboxConsumerReceipt> findConsumerReceipt(String idempotencyKey) {
        return support.queryOptional(
                """
                        select idempotency_key, outbox_event_id, operation_id, event_type,
                               aggregate_type, aggregate_id, received_at
                        from operation_outbox_consumer_receipts
                        where idempotency_key = ?
                        """,
                support.operationOutboxConsumerReceiptMapper(),
                idempotencyKey
        );
    }

    @Override
    public void recordConsumerDeliveryMetric(Instant occurredAt, boolean duplicate) {
        Instant bucketStartedAt = occurredAt.truncatedTo(ChronoUnit.MINUTES);
        if (support.isPostgresDatabase()) {
            jdbcTemplate.update(
                    """
                            insert into operation_outbox_consumer_delivery_metrics (
                                bucket_started_at,
                                processed_delivery_count,
                                duplicate_delivery_count,
                                updated_at
                            )
                            values (?, ?, ?, ?)
                            on conflict (bucket_started_at) do update
                            set processed_delivery_count =
                                    operation_outbox_consumer_delivery_metrics.processed_delivery_count + ?,
                                duplicate_delivery_count =
                                    operation_outbox_consumer_delivery_metrics.duplicate_delivery_count + ?,
                                updated_at = ?
                            """,
                    support.timestamp(bucketStartedAt),
                    duplicate ? 0 : 1,
                    duplicate ? 1 : 0,
                    support.timestamp(occurredAt),
                    duplicate ? 0 : 1,
                    duplicate ? 1 : 0,
                    support.timestamp(occurredAt)
            );
            return;
        }

        int updatedRows = jdbcTemplate.update(
                """
                        update operation_outbox_consumer_delivery_metrics
                        set processed_delivery_count = processed_delivery_count + ?,
                            duplicate_delivery_count = duplicate_delivery_count + ?,
                            updated_at = ?
                        where bucket_started_at = ?
                        """,
                duplicate ? 0 : 1,
                duplicate ? 1 : 0,
                support.timestamp(occurredAt),
                support.timestamp(bucketStartedAt)
        );
        if (updatedRows == 1) {
            return;
        }
        try {
            jdbcTemplate.update(
                    """
                            insert into operation_outbox_consumer_delivery_metrics (
                                bucket_started_at,
                                processed_delivery_count,
                                duplicate_delivery_count,
                                updated_at
                            )
                            values (?, ?, ?, ?)
                            """,
                    support.timestamp(bucketStartedAt),
                    duplicate ? 0 : 1,
                    duplicate ? 1 : 0,
                    support.timestamp(occurredAt)
            );
        } catch (DuplicateKeyException exception) {
            recordConsumerDeliveryMetric(occurredAt, duplicate);
        }
    }

    @Override
    public OperationOutboxConsumerMetrics getConsumerMetrics() {
        return jdbcTemplate.queryForObject(
                """
                        select
                            (select count(*) from operation_outbox_consumer_processed_events) as processed_event_count,
                            (select coalesce(sum(duplicate_count), 0)
                             from operation_outbox_consumer_processed_events) as duplicate_event_count,
                            (select count(*) from operation_outbox_consumer_receipts) as receipt_count,
                            (select max(processed_at)
                             from operation_outbox_consumer_processed_events) as last_processed_at,
                            (select max(received_at)
                             from operation_outbox_consumer_receipts) as last_received_at
                        """,
                (resultSet, rowNumber) -> new OperationOutboxConsumerMetrics(
                        resultSet.getLong("processed_event_count"),
                        resultSet.getLong("duplicate_event_count"),
                        resultSet.getLong("receipt_count"),
                        support.nullableInstant(resultSet, "last_processed_at"),
                        support.nullableInstant(resultSet, "last_received_at")
                )
        );
    }

    @Override
    public OperationOutboxConsumerWindowMetrics getConsumerWindowMetrics(
            Instant windowStartedAt,
            Instant windowEndedAt
    ) {
        return jdbcTemplate.queryForObject(
                """
                        select
                            coalesce(sum(processed_delivery_count), 0) as processed_delivery_count,
                            coalesce(sum(duplicate_delivery_count), 0) as duplicate_delivery_count
                        from operation_outbox_consumer_delivery_metrics
                        where bucket_started_at >= ?
                          and bucket_started_at < ?
                        """,
                (resultSet, rowNumber) -> new OperationOutboxConsumerWindowMetrics(
                        windowStartedAt,
                        windowEndedAt,
                        resultSet.getLong("processed_delivery_count"),
                        resultSet.getLong("duplicate_delivery_count")
                ),
                support.timestamp(windowStartedAt),
                support.timestamp(windowEndedAt)
        );
    }

    @Override
    public List<OperationOutboxConsumerReceipt> findRecentConsumerReceipts(int limit) {
        return jdbcTemplate.query(
                """
                        select idempotency_key, outbox_event_id, operation_id, event_type,
                               aggregate_type, aggregate_id, received_at
                        from operation_outbox_consumer_receipts
                        order by received_at desc, idempotency_key desc
                        limit ?
                        """,
                support.operationOutboxConsumerReceiptMapper(),
                limit
        );
    }

    @Override
    public int deleteConsumerProcessedEventsProcessedBefore(Instant cutoff) {
        return jdbcTemplate.update(
                "delete from operation_outbox_consumer_processed_events where processed_at < ?",
                support.timestamp(cutoff)
        );
    }

    @Override
    public int deleteConsumerReceiptsReceivedBefore(Instant cutoff) {
        return jdbcTemplate.update(
                "delete from operation_outbox_consumer_receipts where received_at < ?",
                support.timestamp(cutoff)
        );
    }

    @Override
    public int deleteConsumerDeliveryMetricsBucketStartedBefore(Instant cutoff) {
        return jdbcTemplate.update(
                "delete from operation_outbox_consumer_delivery_metrics where bucket_started_at < ?",
                support.timestamp(cutoff)
        );
    }
}
