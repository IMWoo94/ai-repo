# 0014. Outbox Processing Lease Recovery

## 스펙 목표

- relay worker가 `PROCESSING` 상태로 claim한 뒤 crash 되어도 event가 영구 고착되지 않게 한다.
- 처리 중 event의 claim 시각과 lease 만료 시각을 기록한다.
- lease 만료 후 같은 event를 다시 claim할 수 있게 한다.

## 완료 결과

- outbox event에 `claimedAt`, `leaseExpiresAt`을 추가했다.
- `claimReadyEvents(limit)`가 claim 시점부터 60초 lease를 부여하도록 했다.
- `PENDING`, retry 가능한 `FAILED`, lease 만료된 `PROCESSING` event를 claim 대상으로 확장했다.
- 발행 성공/실패 시 lease 필드를 초기화하도록 했다.
- Flyway V7 migration으로 `claimed_at`, `lease_expires_at` 컬럼을 추가했다.

## 검증

- `OperationOutboxEventTest`로 `PROCESSING` lease 불변식을 검증했다.
- `OperationOutboxRelayServiceTest`로 lease 만료 전/후 claim 동작을 검증했다.
- `JdbcWalletRepositoryTest`로 JDBC claim recovery와 lease 필드 저장을 검증했다.

## 남은 일

- worker identity와 heartbeat 기반 lease 연장은 아직 없다.
- max attempt, DLQ, manual review 상태는 아직 없다.
- 실제 scheduler/poller와 broker adapter는 후속 작업이다.
- consumer idempotency key와 event schema versioning이 필요하다.

## 관련 문서

- `docs/adr/0017-outbox-processing-lease-recovery.md`
- `issue-drafts/0014-outbox-processing-lease-recovery.md`
- `src/main/java/com/imwoo/airepo/wallet/application/OperationOutboxRelayService.java`
- `src/main/resources/db/migration/V7__add_outbox_processing_lease.sql`
