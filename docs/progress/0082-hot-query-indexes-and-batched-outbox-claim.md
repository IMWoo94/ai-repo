# 0082. Hot Query Indexes and Batched Outbox Claim

## 스펙 목표

- Issue #148의 핫 쿼리 인덱스 부재와 outbox claim 행 단위 루프 부채를 해소한다.
- 지갑별 원장/거래내역 조회와 outbox relay claim 스캔을 인덱스로 커버한다.
- claim UPDATE를 단일 배치로 전환해 relay tick round-trip을 줄인다.

## 완료 결과

- `V19__add_hot_query_indexes.sql` 마이그레이션과 `db/postgresql/schema.sql`에 동일 인덱스를 추가했다.
  - `ledger_entries(wallet_id, occurred_at)`
  - `transaction_history(wallet_id, occurred_at)`
  - `operation_outbox_events(status, occurred_at, outbox_event_id)`
- `JdbcOutboxRelayRepository.claimReadyOutboxEvents`의 행 단위 UPDATE 루프를 `where outbox_event_id in (...)` 단일 배치 UPDATE로 교체했다. status/claimed_at/lease_expires_at/next_retry_at/published_at/last_error 세팅 값과 `for update skip locked` 선행 claim, 트랜잭션 경계는 유지했다.
- `JdbcWalletRepositoryTest`에 N개의 ready 이벤트를 한 번에 claim할 때 전부 `PROCESSING`으로 전이하는지 검증하는 테스트를 추가했다.
- ADR-0071로 인덱스·배치 claim 결정을 기록했다.

## 검증

- `./gradlew compileTestJava`
- `./gradlew test --tests '*JdbcWalletRepositoryTest'`
- `AI_REPO_DEV_RULES_BASE=origin/main bash scripts/check-dev-rules.sh`

## 남은 일

- `PUBLISHED` outbox 행 pruning(보존 기간 기준 삭제)은 이번 범위에서 제외했다. 별도 스케줄러/서비스로 relay run pruning과 같은 축에서 다룬다.

## 관련 문서

- `docs/adr/0071-hot-query-indexes-and-batched-outbox-claim.md`
- `docs/releases/unreleased.md`
- `issue-drafts/0082-hot-query-indexes-and-batched-outbox-claim.md`
