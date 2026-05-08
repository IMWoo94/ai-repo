# 0049. Requeue Rejection Workflow

## 스펙 목표

Requeue 승인 워크플로우에 `REQUESTED -> REJECTED` 반려 경로를 추가한다.

반려자, 반려 사유, 반려 시각을 기록하고, 반려 시 outbox event는 `MANUAL_REVIEW` 상태를 유지한다.

## 완료 결과

- `OperationOutboxRequeueRequestStatus.REJECTED`를 추가했다.
- requeue request에 `rejectedBy`, `rejectedAt`, `rejectionReason` 필드를 추가했다.
- Flyway V12 migration으로 반려 컬럼을 추가했다.
- `POST /api/v1/outbox-events/requeue-requests/{requestId}/reject` API를 추가했다.
- 반려자는 요청자와 달라야 하도록 검증한다.
- React 운영자 콘솔에 반려 사유 입력과 `Requeue 반려` 버튼을 추가했다.
- backend service/API/JDBC test와 frontend unit/E2E를 반려 경로 기준으로 갱신했다.

## 검증

- `./gradlew test --tests "*OperationOutboxRelayServiceTest" --tests "*OperationOutboxReviewControllerTest" --tests "*JdbcWalletRepositoryTest"`
- `scripts/check-dev-rules.sh`
- `./gradlew check`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `scripts/mvp-local-smoke.sh`
- `git diff --check`

## 남은 일

- 실제 로그인/OIDC identity와 반려자 권한 scope 연결은 후속 작업으로 남긴다.
- 반려 후 재요청 정책은 별도 운영 정책으로 검토한다.

## 관련 문서

- `docs/adr/0038-requeue-approval-workflow.md`
- `docs/frontend/react-user-frontend.md`
- `docs/testing/local-test-guide.md`
