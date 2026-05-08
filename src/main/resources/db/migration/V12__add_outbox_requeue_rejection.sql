ALTER TABLE operation_outbox_requeue_requests
    ADD COLUMN IF NOT EXISTS rejected_by VARCHAR(64);

ALTER TABLE operation_outbox_requeue_requests
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE operation_outbox_requeue_requests
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(255);
