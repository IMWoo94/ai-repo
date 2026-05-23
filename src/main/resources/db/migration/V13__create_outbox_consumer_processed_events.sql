CREATE TABLE IF NOT EXISTS operation_outbox_consumer_processed_events (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    outbox_event_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
