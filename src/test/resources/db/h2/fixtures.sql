INSERT INTO members (member_id, status, created_at)
VALUES
    ('member-001', 'ACTIVE', TIMESTAMP '2026-05-01 00:00:00'),
    ('member-002', 'ACTIVE', TIMESTAMP '2026-05-01 00:00:00');

INSERT INTO wallet_accounts (wallet_id, member_id, status, created_at)
VALUES
    ('wallet-001', 'member-001', 'ACTIVE', TIMESTAMP '2026-05-01 00:00:00'),
    ('wallet-002', 'member-002', 'ACTIVE', TIMESTAMP '2026-05-01 00:00:00');

INSERT INTO wallet_balances (wallet_id, amount, currency, as_of)
VALUES
    ('wallet-001', 125000, 'KRW', TIMESTAMP '2026-05-01 00:00:00'),
    ('wallet-002', 30000, 'KRW', TIMESTAMP '2026-05-01 00:00:00');

INSERT INTO transaction_history (
    transaction_id,
    wallet_id,
    occurred_at,
    type,
    status,
    direction,
    amount,
    currency,
    description
)
VALUES
    (
        'txn-002',
        'wallet-001',
        TIMESTAMP '2026-05-01 00:00:00',
        'REWARD',
        'COMPLETED',
        'CREDIT',
        25000,
        'KRW',
        '학습용 리워드 적립'
    ),
    (
        'txn-001',
        'wallet-001',
        TIMESTAMP '2026-04-30 00:00:00',
        'CHARGE',
        'COMPLETED',
        'CREDIT',
        100000,
        'KRW',
        '학습용 충전'
    );
