# 0037. Operator Console E2E Smoke

## 스펙 목표

- 운영자 manual review 콘솔이 브라우저 E2E에서 실제로 렌더링되는지 확인한다.
- 잘못된 admin token이 운영 API 인증 오류로 표시되는지 확인한다.
- local admin header로 manual review 조회 시 empty state가 표시되는지 확인한다.

## 완료 결과

- Playwright E2E에 운영자 콘솔 smoke 시나리오를 추가했다.
- 운영자 `Admin token`, `Operator ID` 기본값을 브라우저에서 검증한다.
- 잘못된 token으로 `ADMIN_AUTHENTICATION_REQUIRED` error state를 검증한다.
- `local-ops-token`으로 manual review 조회 성공 메시지와 empty state를 검증한다.
- local test guide의 E2E 통과 기준과 검증 시나리오 목록을 갱신했다.

## 검증

- `npm --prefix frontend run e2e`
- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `git diff --check`

## 남은 일

- manual review fixture 생성 방식이 정리되면 requeue 성공과 audit trail E2E를 추가한다.
- relay health와 pruning 화면이 운영자 콘솔에 추가되면 별도 E2E 시나리오로 확장한다.

## 관련 문서

- `docs/testing/local-test-guide.md`
- `docs/progress/0022-frontend-e2e-test-pipeline.md`
- `docs/progress/0036-operator-manual-review-console-ui.md`
- `issue-drafts/0037-operator-console-e2e-smoke.md`
