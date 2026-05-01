ALTER TABLE operation_outbox_events
    ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE operation_outbox_events
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE operation_outbox_events
    ADD COLUMN IF NOT EXISTS last_error VARCHAR(255);
