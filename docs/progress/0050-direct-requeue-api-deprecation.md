# 0050. Direct Requeue API Deprecation

## 스펙 목표

manual review requeue의 직접 실행 API를 비활성화해 승인 workflow 우회를 막는다.

requeue는 요청자/승인자/실행자가 분리되는 workflow로만 상태 변경이 가능해야 한다.

## 완료 결과

- `POST /api/v1/outbox-events/{outboxEventId}/requeue`가 `410 Gone`을 반환하도록 변경했다.
- `DIRECT_REQUEUE_API_DEPRECATED` 오류 코드를 추가했다.
- 직접 API 호출은 outbox event 상태와 requeue audit을 변경하지 않는다.
- manual review scenario test를 직접 API 대신 `request -> approve -> execute` 흐름으로 전환했다.
- frontend 동작 변경은 없으므로 운영자 콘솔 E2E는 기존 workflow 검증을 유지했다.
- README, ADR, Wiki, release 후보 문서를 workflow-only 정책 기준으로 갱신했다.

## 검증

- `./gradlew test --tests "*OperationOutboxReviewControllerTest" --tests "*WalletScenarioFlowTest"`
- `scripts/check-dev-rules.sh`
- `./gradlew check`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `git diff --check`

## 남은 일

- 다음 API 정리 시 deprecated endpoint 제거 여부를 결정한다.
- 실제 identity/role scope 도입 시 requester, approver, executor 권한을 더 세분화한다.

## 관련 문서

- `docs/adr/0040-direct-requeue-api-deprecation.md`
- `docs/adr/0038-requeue-approval-workflow.md`
- `docs/testing/local-test-guide.md`
