CREATE TABLE IF NOT EXISTS operation_outbox_requeue_requests (
    request_id VARCHAR(64) PRIMARY KEY,
    outbox_event_id VARCHAR(64) NOT NULL,
    operation_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by VARCHAR(64) NOT NULL,
    request_reason VARCHAR(255) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_by VARCHAR(64),
    approved_at TIMESTAMP WITH TIME ZONE,
    approval_reason VARCHAR(255),
    executed_by VARCHAR(64),
    executed_at TIMESTAMP WITH TIME ZONE
);

CREATE SEQUENCE IF NOT EXISTS outbox_requeue_request_id_seq START WITH 1;
