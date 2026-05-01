# 0013. Outbox Claiming과 Retry

## 스펙 목표

- 여러 relay worker가 동시에 같은 outbox event를 가져가지 않도록 claiming 경계를 만든다.
- 실패 event는 정해진 retry 시각 이후에만 다시 claim 대상이 되게 한다.
- 실제 broker adapter와 scheduler 이전에 안전한 relay 상태 전이를 먼저 검증한다.

## 완료 결과

- outbox status에 `PROCESSING`을 추가했다.
- outbox event에 `nextRetryAt`을 추가했다.
- `claimReadyEvents(limit)` service API를 추가했다.
- PostgreSQL repository는 `FOR UPDATE SKIP LOCKED` 기반으로 ready event를 claim한다.
- 발행 실패 시 고정 30초 backoff를 적용해 `nextRetryAt`을 기록한다.
- Flyway V6 migration으로 `next_retry_at` 컬럼을 추가했다.

## 검증

- `OperationOutboxEventTest`로 `PROCESSING` 상태 불변식을 검증했다.
- `OperationOutboxRelayServiceTest`로 claim, 실패 backoff, retry 가능 시각을 검증했다.
- `JdbcWalletRepositoryTest`로 JDBC claiming과 retry schedule을 검증했다.

## 남은 일

- `PROCESSING` event의 lease timeout과 회수 정책은 아직 없다.
- max attempt와 DLQ 또는 manual review 상태는 아직 없다.
- 실제 scheduler/poller와 broker adapter는 후속 작업이다.
- consumer idempotency key와 event schema versioning이 필요하다.

## 관련 문서

- `docs/adr/0016-outbox-claiming-retry-policy.md`
- `issue-drafts/0013-outbox-claiming-retry.md`
- `src/main/java/com/imwoo/airepo/wallet/application/OperationOutboxRelayService.java`
- `src/main/resources/db/migration/V6__add_outbox_retry_schedule.sql`
