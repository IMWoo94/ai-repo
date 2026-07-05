-- 멱등키를 전역 유니크에서 지갑 스코프로 한정한다(#149).
-- wallet_id 는 NOT NULL 이고 기존 idempotency_key 가 유니크였으므로 복합 PK 도 유니크가 보장된다.
ALTER TABLE wallet_operations DROP CONSTRAINT IF EXISTS wallet_operations_pkey;
ALTER TABLE wallet_operations ADD PRIMARY KEY (wallet_id, idempotency_key);
