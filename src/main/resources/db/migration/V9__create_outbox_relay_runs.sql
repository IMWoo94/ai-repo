CREATE TABLE IF NOT EXISTS operation_outbox_relay_runs (
    relay_run_id VARCHAR(64) PRIMARY KEY,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    batch_size INTEGER NOT NULL,
    claimed_count INTEGER NOT NULL,
    published_count INTEGER NOT NULL,
    failed_count INTEGER NOT NULL,
    error_message VARCHAR(255)
);

CREATE SEQUENCE IF NOT EXISTS outbox_relay_run_id_seq START WITH 1;
