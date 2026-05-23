CREATE TABLE IF NOT EXISTS operational_alerts (
    alert_id VARCHAR(64) PRIMARY KEY,
    source VARCHAR(64) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reasons TEXT NOT NULL
);

CREATE SEQUENCE IF NOT EXISTS operational_alert_id_seq START WITH 1;

CREATE INDEX IF NOT EXISTS idx_operational_alerts_occurred_at_id
    ON operational_alerts (occurred_at DESC, alert_id DESC);
