# 0016. Outbox Manual Review API

## 스펙 목표

- 자동 retry에서 제외된 `MANUAL_REVIEW` event를 운영자가 조회할 수 있게 한다.
- 원인 조치 후 manual review event를 다시 자동 처리 흐름으로 넣을 수 있게 한다.
- DB 직접 수정이 아니라 API와 테스트로 requeue 정책을 고정한다.

## 완료 결과

- `GET /api/v1/outbox-events/manual-review` API를 추가했다.
- `POST /api/v1/outbox-events/{outboxEventId}/requeue` API를 추가했다.
- requeue 시 `MANUAL_REVIEW` event를 `PENDING`으로 전환한다.
- requeue 시 `attemptCount`, retry/lease/publish/error 필드를 초기화한다.
- requeue된 event는 다시 claim 대상이 된다.

## 검증

- `OperationOutboxReviewControllerTest`로 조회와 requeue API를 검증했다.
- `OperationOutboxRelayServiceTest`로 service requeue 정책을 검증했다.
- `JdbcWalletRepositoryTest`로 JDBC requeue와 재claim 가능성을 검증했다.

## 남은 일

- 인증/인가가 아직 없다.
- requeue 승인자, 사유, 감사 이력 테이블이 아직 없다.
- 알림/모니터링 연동은 아직 없다.
- 실제 broker DLQ replay는 아직 없다.

## 관련 문서

- `docs/adr/0019-outbox-manual-review-api.md`
- `issue-drafts/0016-outbox-manual-review-api.md`
- `src/main/java/com/imwoo/airepo/wallet/api/OperationOutboxReviewController.java`
- `src/main/java/com/imwoo/airepo/wallet/application/OperationOutboxRelayService.java`
