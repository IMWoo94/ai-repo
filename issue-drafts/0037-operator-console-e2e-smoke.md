# [Test] 운영자 콘솔 E2E smoke 추가

## 배경

운영자 manual review 콘솔은 UI와 프론트 단위 테스트가 추가되었지만, MVP 출시 관점에서는 브라우저에서 운영자 영역이 실제로 렌더링되고 운영 API 인증 오류와 empty state가 보이는지 확인하는 E2E gate가 필요하다.

## 목표

- Playwright E2E에서 운영자 콘솔 렌더링을 검증한다.
- 잘못된 admin token 입력 시 운영 API 인증 오류가 화면에 표시되는지 검증한다.
- local admin token으로 manual review 조회 시 empty state가 화면에 표시되는지 검증한다.

## 범위

- `frontend/e2e/wallet-flow.spec.ts`에 운영자 콘솔 smoke 시나리오를 추가한다.
- `docs/testing/local-test-guide.md`의 E2E 통과 기준을 갱신한다.
- 진행 흔적 문서를 추가한다.

## 범위 제외

- manual review event fixture 생성
- requeue 성공 E2E
- requeue audit trail full E2E
- 백엔드 API 계약 변경

## 인수 조건

- [x] 운영자 콘솔 heading이 브라우저 E2E에서 보인다.
- [x] 기본 `Admin token`, `Operator ID` 값이 보인다.
- [x] 잘못된 admin token으로 조회하면 `ADMIN_AUTHENTICATION_REQUIRED`가 보인다.
- [x] `local-ops-token`으로 조회하면 manual review empty state가 보인다.
- [x] local test guide가 현재 E2E 시나리오 수와 검증 대상을 반영한다.

## 테스트

- `npm --prefix frontend run e2e`
- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `git diff --check`

## 리스크와 트레이드오프

- requeue 성공까지 E2E로 묶으려면 manual review 상태를 안정적으로 만드는 fixture가 필요하다.
- 이번 단계에서는 fixture 없이 항상 재현 가능한 운영자 콘솔 smoke를 먼저 고정한다.
- full requeue E2E는 상태 fixture 정책을 별도 작업으로 정리한 뒤 추가한다.
