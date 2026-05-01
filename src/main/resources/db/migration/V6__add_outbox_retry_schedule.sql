ALTER TABLE operation_outbox_events
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP WITH TIME ZONE;
