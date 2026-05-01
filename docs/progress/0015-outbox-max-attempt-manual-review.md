# 0015. Outbox Max Attempt와 Manual Review

## 스펙 목표

- 반복 실패 outbox event가 무한 재시도되지 않게 한다.
- 최대 시도 횟수에 도달한 event를 자동 retry 흐름에서 분리한다.
- 실제 DLQ broker 이전에 manual review 상태를 먼저 도입한다.

## 완료 결과

- outbox status에 `MANUAL_REVIEW`를 추가했다.
- relay 실패 처리는 최대 3회 시도 정책을 사용한다.
- 1~2회 실패는 `FAILED`와 `nextRetryAt`을 유지한다.
- 3회 실패는 `MANUAL_REVIEW`로 전이하고 `nextRetryAt`을 제거한다.
- `MANUAL_REVIEW` event는 자동 claim 대상에서 제외된다.

## 검증

- `OperationOutboxEventTest`로 `MANUAL_REVIEW` 불변식을 검증했다.
- `OperationOutboxRelayServiceTest`로 max attempt 이후 claim 제외를 검증했다.
- `JdbcWalletRepositoryTest`로 JDBC 실패 전이와 manual review 격리를 검증했다.

## 남은 일

- manual review 조회/재처리 API는 아직 없다.
- 실제 DLQ broker topic/queue는 아직 없다.
- 알림과 모니터링 연동은 아직 없다.
- event schema versioning과 consumer idempotency key가 필요하다.

## 관련 문서

- `docs/adr/0018-outbox-max-attempt-manual-review.md`
- `issue-drafts/0015-outbox-max-attempt-manual-review.md`
- `src/main/java/com/imwoo/airepo/wallet/application/OperationOutboxRelayService.java`
- `src/main/java/com/imwoo/airepo/wallet/domain/OperationOutboxStatus.java`
