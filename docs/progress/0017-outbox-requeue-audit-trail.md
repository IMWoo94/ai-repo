# 0017. Outbox Requeue Audit Trail

## 스펙 목표

- manual review event를 requeue한 운영 조치의 흔적을 남긴다.
- operator, reason, requeuedAt을 구조화된 감사 이력으로 저장한다.
- requeue 이력을 API로 조회할 수 있게 한다.

## 완료 결과

- requeue 요청 body에 `operator`, `reason`을 추가했다.
- `OperationOutboxRequeueAudit` 도메인 모델을 추가했다.
- `operation_outbox_requeue_audits` 테이블을 추가했다.
- requeue 성공 시 event 상태 변경과 감사 이력 저장을 함께 수행한다.
- `GET /api/v1/outbox-events/{outboxEventId}/requeue-audits` API를 추가했다.

## 검증

- `OperationOutboxRequeueAuditTest`로 감사 이력 필수값을 검증했다.
- `OperationOutboxReviewControllerTest`로 requeue 요청 body와 감사 이력 조회 API를 검증했다.
- `OperationOutboxRelayServiceTest`로 service requeue 감사 이력을 검증했다.
- `JdbcWalletRepositoryTest`로 JDBC requeue 감사 이력 저장을 검증했다.

## 남은 일

- operator가 실제 인증 사용자와 연결되지 않는다.
- 승인 워크플로우가 아직 없다.
- 알림/모니터링 연동은 아직 없다.

## 관련 문서

- `docs/adr/0020-outbox-requeue-audit-trail.md`
- `issue-drafts/0017-outbox-requeue-audit-trail.md`
- `src/main/java/com/imwoo/airepo/wallet/domain/OperationOutboxRequeueAudit.java`
- `src/main/resources/db/migration/V8__create_outbox_requeue_audits.sql`
