# 0047. Requeue Approval Workflow

## 스펙 목표

manual review requeue를 단일 admin 실행에서 요청자/승인자/실행자 분리 흐름으로 확장한다.

상태 모델은 `REQUESTED -> APPROVED -> EXECUTED`를 먼저 사용한다.

## 완료 결과

- `operation_outbox_requeue_requests` 테이블과 Flyway migration을 추가했다.
- requeue 요청, 승인, 실행 API를 추가했다.
- 승인자는 요청자와 달라야 하도록 검증한다.
- 실행 단계에서만 outbox event를 `PENDING`으로 되돌리고 기존 requeue audit을 남긴다.
- 운영자 콘솔을 requeue 요청, 승인, 실행 단계로 변경했다.
- backend service/API/JDBC test와 frontend unit/E2E를 승인 워크플로우 기준으로 갱신했다.

## 검증

- `./gradlew test --tests "*OperationOutboxRelayServiceTest" --tests "*OperationOutboxReviewControllerTest" --tests "*JdbcWalletRepositoryTest"`
- `./gradlew check`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `scripts/mvp-local-smoke.sh`
- `git diff --check`

## 남은 일

- 직접 requeue API는 ADR-0040에서 deprecated로 결정했다.
- 승인 반려 상태는 0049 단계에서 추가했다.
- 실제 로그인/OIDC identity와 승인자 권한 scope 연결은 후속 작업으로 남긴다.

## 관련 문서

- `docs/adr/0038-requeue-approval-workflow.md`
- `docs/frontend/react-user-frontend.md`
- `docs/testing/local-test-guide.md`
