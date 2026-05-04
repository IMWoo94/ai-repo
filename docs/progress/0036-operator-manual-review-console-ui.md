# 0036. Operator Manual Review Console UI

## 스펙 목표

- 운영자가 화면에서 manual review outbox를 조회한다.
- 선택한 event를 reason과 함께 requeue한다.
- requeue audit trail을 화면에서 확인한다.
- Apple 스타일 기준으로 card, status badge, empty/error state를 정리한다.

## 완료 결과

- React 화면에 `Operator console` 섹션을 추가했다.
- `X-Admin-Token`, `X-Operator-Id` 입력 UI를 추가했다.
- `GET /api/v1/outbox-events/manual-review` 조회 UI를 추가했다.
- `POST /api/v1/outbox-events/{outboxEventId}/requeue` 실행 UI를 추가했다.
- `GET /api/v1/outbox-events/{outboxEventId}/requeue-audits` 조회 UI를 추가했다.
- manual review empty state와 API error callout을 추가했다.
- outbox status badge와 운영자 card layout을 Apple 스타일 기준으로 정리했다.
- 프론트 unit test에 empty state, requeue, audit trail 검증을 추가했다.

## 검증

- `cd frontend && npm run test`
- `cd frontend && npm run build`
- `cd frontend && npm run e2e`
- `git diff --check`

## 남은 일

- 운영자 승인 워크플로우는 아직 없다.
- operator/admin role 분리와 실제 로그인 연동은 후속 작업이다.
- 운영자 콘솔 smoke E2E는 추가되었고, requeue 성공 E2E는 manual review fixture 생성 방식이 정리된 뒤 추가한다.
- relay health와 pruning 화면은 별도 운영자 콘솔 확장 작업으로 분리한다.

## 관련 문서

- `docs/frontend/react-user-frontend.md`
- `issue-drafts/0036-operator-manual-review-console-ui.md`
- `docs/adr/0024-react-user-frontend-mvp.md`
- `docs/adr/0019-outbox-manual-review-api.md`
- `docs/adr/0020-outbox-requeue-audit-trail.md`
