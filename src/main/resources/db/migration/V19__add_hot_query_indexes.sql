-- 지갑 원장·거래 내역 조회와 outbox claim 스캔의 핫 경로를 인덱스로 커버한다.
CREATE INDEX IF NOT EXISTS idx_ledger_entries_wallet_id_occurred_at
    ON ledger_entries (wallet_id, occurred_at);

CREATE INDEX IF NOT EXISTS idx_transaction_history_wallet_id_occurred_at
    ON transaction_history (wallet_id, occurred_at);

-- claim 쿼리는 status로 필터하고 occurred_at, outbox_event_id 순으로 정렬한다.
CREATE INDEX IF NOT EXISTS idx_operation_outbox_events_status_occurred_at
    ON operation_outbox_events (status, occurred_at, outbox_event_id);
