# 0046. Top3 Operational Hardening

## 스펙 목표

전면 검토에서 우선순위 Top 3로 뽑은 항목을 순서대로 반영한다.

1. manual review requeue 성공 E2E fixture
2. relay health/pruning 운영자 화면
3. operator/admin token 분리와 role model 강화

## 완료 결과

- E2E 전용 manual review fixture API를 추가했다.
- Playwright에서 manual review event 생성, 조회, requeue, audit trail 확인까지 검증한다.
- 운영자 콘솔에 relay health summary, 최근 relay run, operational log pruning 결과 화면을 추가했다.
- `X-Operator-Token`을 추가하고 operator/admin token을 분리했다.
- operator token은 조회성 운영 API, admin token은 requeue/pruning 같은 변경성 운영 조치에 사용한다.
- smoke script와 README/local guide/frontend/Wiki 문서를 새 header contract에 맞게 갱신했다.

## 검증

- `./gradlew check`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `./gradlew test --tests "*AdminHeaderAuthenticationFilterTest" --tests "*OperationOutboxReviewControllerTest" --tests "*OperationalLogPruningControllerTest" --tests "*TestFixtureControllerTest"`
- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `scripts/mvp-local-smoke.sh`
- `git diff --check`

## 남은 일

- requeue 승인 워크플로우는 별도 작업으로 진행한다.
- external alert channel과 consumer idempotency는 후속 후보로 유지한다.
- pruning 실행 이력 저장은 별도 운영 이력 작업으로 분리한다.

## 관련 문서

- `docs/adr/0037-operator-admin-token-split.md`
- `docs/reviews/2026-05-06-top3-documentation-implementation-verification.md`
- `docs/reviews/2026-05-06-full-review-improvement-list.md`
- `docs/frontend/react-user-frontend.md`
- `docs/testing/local-test-guide.md`
