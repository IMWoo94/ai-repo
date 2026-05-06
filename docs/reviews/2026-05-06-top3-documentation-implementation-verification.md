# 2026-05-06 Top3 Documentation Implementation Verification

## 검증 대상

- PR: #99 `feat: harden operator console flows`
- Issue: #98 Top3 operational hardening
- 범위:
  - manual review requeue 성공 E2E fixture
  - relay health/pruning 운영자 화면
  - operator/admin token 분리와 role model 강화

## 결론

문서와 구현은 현재 PR 기준으로 정합하다.

다만 과거 ADR과 과거 progress 문서는 당시 시점의 의사결정 기록이므로 최신 동작과 다르게 읽힐 수 있다. 최신 운영 계약은 README, ADR-0037, progress 0046, local test guide, Wiki draft를 기준으로 판단한다.

## 구현 대조

| 검증 항목 | 구현 근거 | 문서 근거 | 판정 |
| --- | --- | --- | --- |
| E2E 전용 manual review fixture | `TestFixtureController`가 `ai-repo.test-fixtures.enabled=true`일 때만 활성화된다 | progress 0046, issue draft 0046 | PASS |
| requeue 성공 E2E | Playwright가 fixture 생성, manual review 조회, requeue, audit trail 표시를 검증한다 | local test guide, QA scenarios | PASS |
| relay health 운영자 화면 | React operator console이 health summary와 최근 relay run을 조회한다 | frontend guide, release notes, Wiki draft | PASS |
| pruning 운영자 화면 | React operator console이 pruning API를 실행하고 삭제 건수와 cutoff를 표시한다 | frontend guide, release notes, Wiki draft | PASS |
| operator/admin token 분리 | `X-Operator-Token`은 `ROLE_OPERATOR`, `X-Admin-Token`은 `ROLE_OPERATOR`, `ROLE_ADMIN`을 부여한다 | README, ADR-0037, local setup guide | PASS |
| 변경성 운영 조치 권한 | requeue와 pruning POST는 `ROLE_ADMIN`을 요구한다 | ADR-0037, local test guide | PASS |

## 테스트 대조

| 테스트 | 확인 내용 | 판정 |
| --- | --- | --- |
| `AdminHeaderAuthenticationFilterTest` | operator token은 operator role만 가진다 | PASS |
| `OperationOutboxReviewControllerTest` | operator token만으로 requeue가 거부된다 | PASS |
| `OperationalLogPruningControllerTest` | operator token만으로 pruning이 거부된다 | PASS |
| `TestFixtureControllerTest` | fixture가 manual review outbox event를 만든다 | PASS |
| `frontend/src/App.test.tsx` | relay health와 pruning 결과가 운영자 콘솔에 표시된다 | PASS |
| `frontend/e2e/wallet-flow.spec.ts` | requeue 성공과 audit trail, relay/pruning UI가 브라우저에서 검증된다 | PASS |

## 확인된 문서 보정

- `wiki-drafts/Architecture-Decisions.md`의 다음 후보에서 이미 완료된 relay health/pruning UI와 requeue full E2E fixture를 제거했다.
- `docs/reviews/current-validation-documentation-audit.md`와 `docs/reviews/current-development-baseline-review.md`에 기준 시점 안내를 추가했다.

## 남은 리스크

- test fixture API는 기본 비활성화지만, 활성화되면 인증 없이 상태를 만든다. 로컬 E2E 전용 설정으로만 사용해야 한다.
- UI는 로컬 실습 편의상 operator/admin token을 같은 콘솔에서 입력한다. 실제 운영에서는 로그인 기반 identity, role scope, 승인 워크플로우로 분리해야 한다.
- 과거 ADR의 header 표기는 당시 결정 기록이므로 최신 계약은 ADR-0037을 우선한다.

## 검증 명령

- `./gradlew check`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `scripts/mvp-local-smoke.sh`
- `git diff --check`
