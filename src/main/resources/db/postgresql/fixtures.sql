INSERT INTO members (member_id, status, created_at)
VALUES
    ('member-001', 'ACTIVE', TIMESTAMP WITH TIME ZONE '2026-05-01 00:00:00Z'),
    ('member-002', 'ACTIVE', TIMESTAMP WITH TIME ZONE '2026-05-01 00:00:00Z')
ON CONFLICT (member_id) DO NOTHING;

INSERT INTO wallet_accounts (wallet_id, member_id, status, created_at)
VALUES
    ('wallet-001', 'member-001', 'ACTIVE', TIMESTAMP WITH TIME ZONE '2026-05-01 00:00:00Z'),
    ('wallet-002', 'member-002', 'ACTIVE', TIMESTAMP WITH TIME ZONE '2026-05-01 00:00:00Z')
ON CONFLICT (wallet_id) DO NOTHING;

INSERT INTO wallet_balances (wallet_id, amount, currency, as_of)
VALUES
    ('wallet-001', 125000, 'KRW', TIMESTAMP WITH TIME ZONE '2026-05-01 00:00:00Z'),
    ('wallet-002', 30000, 'KRW', TIMESTAMP WITH TIME ZONE '2026-05-01 00:00:00Z')
ON CONFLICT (wallet_id) DO NOTHING;

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
        TIMESTAMP WITH TIME ZONE '2026-05-01 00:00:00Z',
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
        TIMESTAMP WITH TIME ZONE '2026-04-30 00:00:00Z',
        'CHARGE',
        'COMPLETED',
        'CREDIT',
        100000,
        'KRW',
        '학습용 충전'
    )
ON CONFLICT (transaction_id) DO NOTHING;
