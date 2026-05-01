CREATE TABLE IF NOT EXISTS members (
    member_id VARCHAR(64) PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS wallet_accounts (
    wallet_id VARCHAR(64) PRIMARY KEY,
    member_id VARCHAR(64) NOT NULL REFERENCES members(member_id),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS wallet_balances (
    wallet_id VARCHAR(64) PRIMARY KEY REFERENCES wallet_accounts(wallet_id),
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    as_of TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS transaction_history (
    transaction_id VARCHAR(64) PRIMARY KEY,
    wallet_id VARCHAR(64) NOT NULL REFERENCES wallet_accounts(wallet_id),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS wallet_operations (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    fingerprint VARCHAR(512) NOT NULL,
    operation_id VARCHAR(64) NOT NULL,
    transaction_id VARCHAR(64) NOT NULL,
    wallet_id VARCHAR(64) NOT NULL,
    counterparty_wallet_id VARCHAR(64),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_wallet_id VARCHAR(64) NOT NULL,
    balance_amount NUMERIC(19, 2) NOT NULL,
    balance_currency VARCHAR(3) NOT NULL,
    balance_as_of TIMESTAMP WITH TIME ZONE NOT NULL,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    ledger_entry_id VARCHAR(64) PRIMARY KEY,
    operation_id VARCHAR(64) NOT NULL,
    wallet_id VARCHAR(64) NOT NULL REFERENCES wallet_accounts(wallet_id),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    type VARCHAR(32) NOT NULL,
    direction VARCHAR(32) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_after_amount NUMERIC(19, 2) NOT NULL,
    balance_after_currency VARCHAR(3) NOT NULL,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_events (
    audit_event_id VARCHAR(64) PRIMARY KEY,
    operation_id VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    detail VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS operation_step_logs (
    operation_step_log_id VARCHAR(64) PRIMARY KEY,
    operation_id VARCHAR(64) NOT NULL,
    step VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    detail VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS operation_outbox_events (
    outbox_event_id VARCHAR(64) PRIMARY KEY,
    operation_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(255)
);

CREATE SEQUENCE IF NOT EXISTS transaction_id_seq START WITH 3;
CREATE SEQUENCE IF NOT EXISTS operation_id_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS ledger_entry_id_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS audit_event_id_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS operation_step_log_id_seq START WITH 1;
CREATE SEQUENCE IF NOT EXISTS outbox_event_id_seq START WITH 1;
