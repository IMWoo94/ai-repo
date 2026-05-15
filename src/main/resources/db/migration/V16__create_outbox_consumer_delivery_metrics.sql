CREATE TABLE IF NOT EXISTS operation_outbox_consumer_delivery_metrics (
    bucket_started_at TIMESTAMP WITH TIME ZONE PRIMARY KEY,
    processed_delivery_count BIGINT NOT NULL DEFAULT 0,
    duplicate_delivery_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
