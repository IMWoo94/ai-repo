CREATE TABLE IF NOT EXISTS operation_step_logs (
    operation_step_log_id VARCHAR(64) PRIMARY KEY,
    operation_id VARCHAR(64) NOT NULL,
    step VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    detail VARCHAR(255) NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS operation_step_log_id_seq START WITH 1;
