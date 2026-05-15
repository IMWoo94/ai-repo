ALTER TABLE operation_outbox_consumer_processed_events
    ADD COLUMN IF NOT EXISTS duplicate_count INTEGER NOT NULL DEFAULT 0;
