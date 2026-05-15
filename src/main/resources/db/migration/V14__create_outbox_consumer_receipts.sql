CREATE TABLE IF NOT EXISTS operation_outbox_consumer_receipts (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    outbox_event_id VARCHAR(64) NOT NULL,
    operation_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);
