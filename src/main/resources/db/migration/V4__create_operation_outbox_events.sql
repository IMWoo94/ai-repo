CREATE TABLE IF NOT EXISTS operation_outbox_events (
    outbox_event_id VARCHAR(64) PRIMARY KEY,
    operation_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS outbox_event_id_seq START WITH 1;
