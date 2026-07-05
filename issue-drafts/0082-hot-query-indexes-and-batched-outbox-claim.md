# 핫 쿼리 인덱스 추가 + outbox claim 배치 업데이트

GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/148

## 증상

- `ledger_entries`·`transaction_history`는 `wallet_id` 기준 시간순 조회가 핫 경로인데 인덱스가 없어 데이터가 커지면 전체 스캔이 발생한다.
- `operation_outbox_events` relay claim은 `status` 필터 + `occurred_at, outbox_event_id` 정렬인데 인덱스가 없어 이벤트가 쌓이면 정렬/스캔 비용이 커진다.
- `JdbcOutboxRelayRepository.claimReadyOutboxEvents`가 claim 대상 id마다 `update ... where outbox_event_id = ?`를 행 단위로 반복해 배치 크기만큼 round-trip이 발생한다.

## 결정

- `ledger_entries(wallet_id, occurred_at)`, `transaction_history(wallet_id, occurred_at)`, `operation_outbox_events(status, occurred_at, outbox_event_id)` 인덱스를 `V19` 마이그레이션과 통합 스키마에 추가한다.
- claim UPDATE 루프를 `where outbox_event_id in (...)` 단일 배치로 교체하되 세팅 값·`for update skip locked` 선행 claim·트랜잭션 경계를 유지한다.
- `PUBLISHED` 행 pruning은 이번 범위에서 제외하고 후속 과제로 둔다.

## 검증

```bash
./gradlew compileTestJava
./gradlew test --tests '*JdbcWalletRepositoryTest'
AI_REPO_DEV_RULES_BASE=origin/main bash scripts/check-dev-rules.sh
```

## 관련 문서

- `docs/adr/0071-hot-query-indexes-and-batched-outbox-claim.md`
- `docs/progress/0082-hot-query-indexes-and-batched-outbox-claim.md`
- `docs/releases/unreleased.md`
