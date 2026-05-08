# ADR-0042: Outbox Claim Guarded Result Update

## 상태

Accepted

## 배경

Outbox relay는 event를 `PROCESSING`으로 claim하고 lease를 기록한다. Lease가 만료되면 다른 worker가 같은 event를 다시 claim할 수 있다.

기존 publish 결과 갱신은 outbox id만으로 `PUBLISHED` 또는 `FAILED`를 기록했다. 따라서 첫 worker가 lease 만료 후 늦게 돌아오면, 이미 재claim된 event 상태를 덮어쓸 수 있다.

## 결정

relay publish loop의 결과 갱신은 claim 시점의 `claimedAt`, `leaseExpiresAt`을 claim token처럼 사용한다.

- publish 성공 갱신은 `status = PROCESSING`, `claimed_at`, `lease_expires_at`이 모두 일치할 때만 `PUBLISHED`로 전이한다.
- publish 실패 갱신도 동일 조건에서만 `FAILED` 또는 `MANUAL_REVIEW`로 전이한다.
- 조건부 update가 1건이 아니면 `InvalidWalletOperationException`으로 실패한다.
- 기존 수동 테스트/fixture용 `markPublished`, `markFailed` API는 유지하되 실제 `publishReadyEvents` 경로는 guarded update만 사용한다.
- H2 repository test와 PostgreSQL Testcontainers test에서 stale writer가 재claim된 event를 덮어쓰지 못하는지 검증한다.

## 트레이드오프

### 장점

- lease 만료 후 늦은 worker가 재claim된 event를 `PUBLISHED`나 `FAILED`로 덮지 못한다.
- worker identity 없이도 현재 schema의 `claimedAt`, `leaseExpiresAt`만으로 최소 claim token을 만들 수 있다.
- 실제 broker adapter 도입 전 outbox relay의 핵심 정합성 위험을 줄인다.

### 비용

- claim timestamp가 결과 갱신 계약의 일부가 되어 repository API가 늘어난다.
- 완전한 중복 발행 방지는 아니다. 늦은 worker가 이미 외부 broker에 publish한 뒤 DB 갱신만 실패할 수 있다.
- broker/consumer idempotency 계약은 ADR-0043에서 별도 처리한다.

## 검증 기준

- 첫 claim의 lease가 만료된 뒤 두 번째 claim이 성공한다.
- 첫 worker의 늦은 publish success update는 실패한다.
- 첫 worker의 늦은 publish failure update는 실패한다.
- 실패 후 DB event는 두 번째 claim의 `PROCESSING` 상태와 lease 정보를 유지한다.
- PostgreSQL Testcontainers에서 같은 조건을 검증한다.

## 후속 작업

- worker identity 또는 claim token 컬럼을 명시적으로 도입할지 검토한다.
- consumer processed-event table과 dedupe 보관 기간을 설계한다.
