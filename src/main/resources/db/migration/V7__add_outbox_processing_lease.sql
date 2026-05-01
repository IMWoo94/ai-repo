ALTER TABLE operation_outbox_events
    ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE operation_outbox_events
    ADD COLUMN IF NOT EXISTS lease_expires_at TIMESTAMP WITH TIME ZONE;
