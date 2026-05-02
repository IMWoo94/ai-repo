CREATE TABLE admin_api_access_audits (
    audit_id VARCHAR(64) PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    method VARCHAR(16) NOT NULL,
    path VARCHAR(255) NOT NULL,
    operator_id VARCHAR(64),
    status_code INTEGER NOT NULL,
    outcome VARCHAR(32) NOT NULL
);

CREATE SEQUENCE admin_api_access_audit_id_seq START WITH 1;
