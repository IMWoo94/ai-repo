# 0023. Local Test Guide

## 스펙 목표

- 백엔드/프론트/E2E 테스트 실행 방법을 한 문서로 정리한다.
- 로컬 E2E 시연 결과와 통과 기준을 문서화한다.
- CI job과 로컬 명령의 대응 관계를 명확히 한다.

## 완료 결과

- `docs/testing/local-test-guide.md`를 추가했다.
- 백엔드 `test`, `scenarioTest`, `check`의 목적과 실행 기준을 정리했다.
- 프론트 `build`, `e2e`, `e2e:headed` 실행 기준을 정리했다.
- Playwright E2E가 Spring Boot와 Vite 서버를 자동 실행한다는 점을 문서화했다.
- README, React frontend 문서, E2E strategy 문서에서 새 가이드로 연결했다.

## 검증

- `npm --prefix frontend run e2e`로 로컬 E2E 시연을 확인했다.
- 문서에는 2026-05-02 로컬 시연 통과 기준을 기록했다.

## 남은 일

- 송금 성공/실패 E2E가 추가되면 가이드의 현재 E2E 시나리오 목록을 갱신한다.
- 운영자 manual review 화면이 추가되면 운영자 E2E 실행 기준을 별도 섹션으로 확장한다.

## 관련 문서

- `docs/testing/local-test-guide.md`
- `docs/testing/frontend-e2e-test-strategy.md`
- `docs/testing/scenario-test-strategy.md`
- `issue-drafts/0023-local-test-guide.md`
