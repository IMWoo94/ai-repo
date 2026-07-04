CREATE TABLE IF NOT EXISTS audit_event_wallets (
    audit_event_id VARCHAR(64) NOT NULL REFERENCES audit_events(audit_event_id),
    wallet_id VARCHAR(64) NOT NULL REFERENCES wallet_accounts(wallet_id),
    PRIMARY KEY (audit_event_id, wallet_id)
);

CREATE INDEX IF NOT EXISTS idx_audit_event_wallets_wallet_id
    ON audit_event_wallets (wallet_id);

-- 기존 audit_events를 ledger_entries 조인으로 역채움해 로컬 DB 호환성을 유지한다.
INSERT INTO audit_event_wallets (audit_event_id, wallet_id)
SELECT DISTINCT ae.audit_event_id, le.wallet_id
FROM audit_events ae
JOIN ledger_entries le ON le.operation_id = ae.operation_id
ON CONFLICT DO NOTHING;
