CREATE TABLE IF NOT EXISTS operation_outbox_requeue_audits (
    audit_id VARCHAR(64) PRIMARY KEY,
    outbox_event_id VARCHAR(64) NOT NULL,
    operation_id VARCHAR(64) NOT NULL,
    requeued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    operator_name VARCHAR(64) NOT NULL,
    reason VARCHAR(255) NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS outbox_requeue_audit_id_seq START WITH 1;
