# [Feature] Top3 operational hardening

## 배경

2026-05-06 전면 검토에서 우선순위 Top 3로 다음 항목이 선정되었다.

1. manual review requeue 성공 E2E fixture
2. relay health/pruning 운영자 화면
3. operator/admin token 분리와 role model 강화

## 목표

- Playwright에서 manual review event 생성, requeue, audit trail 확인까지 검증한다.
- 운영자 콘솔에서 relay health, relay run, pruning 결과를 확인한다.
- operator token과 admin token을 분리하고 변경성 운영 조치는 admin 권한으로 보호한다.

## 범위

- test fixture API 추가
- frontend operator console 확장
- Spring Security header role model 강화
- smoke script와 문서 갱신

## 범위 제외

- requeue 승인 워크플로우
- 실제 로그인/JWT
- pruning 실행 이력 저장
- external alert channel

## 인수 조건

- [x] requeue 성공 E2E가 추가된다.
- [x] relay health/pruning 운영자 화면과 component/E2E 검증이 추가된다.
- [x] operator/admin token split ADR과 401/403 test matrix가 추가된다.
- [x] README, local guide, Wiki draft, progress가 갱신된다.

## 검증

- `./gradlew test --tests "*AdminHeaderAuthenticationFilterTest" --tests "*OperationOutboxReviewControllerTest" --tests "*OperationalLogPruningControllerTest" --tests "*TestFixtureControllerTest"`
- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `scripts/mvp-local-smoke.sh`
- `git diff --check`

## 리스크와 트레이드오프

- test fixture API는 `ai-repo.test-fixtures.enabled=true`일 때만 활성화한다.
- operator/admin token은 실제 로그인 전 단계의 local header contract다.
- admin token은 하위 호환을 위해 operator role도 포함하지만, operator token은 admin role을 갖지 않는다.
