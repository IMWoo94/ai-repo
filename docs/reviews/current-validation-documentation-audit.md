# Current Validation Documentation Audit

## 점검 일자

- 2026-05-04

## 점검 범위

- 현재 기준 브랜치: `main`
- 최신 병합 PR: #74 `test: add PostgreSQL scenario CI gate`
- 연결 Issue: #73
- 점검 대상:
  - README
  - ADR
  - Progress Reports
  - Issue drafts
  - GitHub Issue/PR/CI 상태
  - Local/Scenario/PostgreSQL/Frontend 테스트 가이드
  - Wiki drafts
  - Release notes

## 결론

현재 개발분의 핵심 검증 문서는 대부분 갖춰져 있다.

다만 포트폴리오형 운영 하네스 관점에서는 다음 문서 공백이 남아 있다.

| 등급 | 항목 | 판단 |
| --- | --- | --- |
| P1 | Wiki draft 최신화 | `wiki-drafts/Domain-Rules.md`와 Wiki index 후속 보강 완료 |
| P1 | 다음 릴리스 후보 문서 | `docs/releases/unreleased.md`로 후속 보강 완료 |
| P2 | QA 시나리오 Wiki | `wiki-drafts/QA-Scenarios.md`로 후속 보강 완료 |
| P2 | Architecture Decisions Wiki | `wiki-drafts/Architecture-Decisions.md`로 후속 보강 완료 |
| P2 | MCP and Skills Wiki | `wiki-drafts/MCP-and-Skills.md`로 후속 보강 완료 |
| P3 | `.dev/rules` 기반 자동 체크 규칙 | `check` 스킬 기준 rule directory가 없음 |

## 확인된 정상 항목

### README

- 프로젝트 목적, 배경, 기술 선택, 하네스 운영 모델이 정리되어 있다.
- 테스트 게이트에 `test`, `scenarioTest`, `postgresScenarioTest`, frontend unit/build/e2e가 반영되어 있다.
- ADR-0036까지 링크가 연결되어 있다.

### ADR

- ADR-0001부터 ADR-0036까지 존재한다.
- 최신 PostgreSQL scenario CI 결정은 ADR-0036에 기록되어 있다.
- Spring Boot 4의 `spring-boot-flyway` 모듈 필요성도 ADR-0036에 반영되어 있다.

### Progress Reports

- 0000부터 0035까지 단계별 완료 흔적이 있다.
- 최신 완료 기능은 PostgreSQL Scenario Testcontainers CI로 갱신되어 있다.
- 각 progress 문서는 목표, 완료 결과, 검증, 남은 일을 포함한다.

### Issue / PR / CI

- Issue #73은 닫힘 상태다.
- PR #74는 병합 완료 상태다.
- PR #74의 CI check는 모두 성공했다.
  - Gradle Check
  - Scenario Test
  - PostgreSQL Scenario Test
  - Frontend Build
  - Frontend Unit Test
  - Frontend E2E

### 테스트 가이드

- `docs/testing/local-test-guide.md`에 `postgresScenarioTest`와 CI 대응 관계가 반영되어 있다.
- `docs/testing/scenario-test-strategy.md`에 `postgres-scenario` tag와 `PostgresWalletScenarioFlowTest`가 반영되어 있다.
- `docs/development/local-setup.md`에 Docker/Testcontainers 필요 조건이 반영되어 있다.

## 누락 또는 보완 후보

### P1. Wiki draft 최신화

`wiki-drafts/Domain-Rules.md` 일부 문장은 최신 구현과 맞지 않는다.

예:

- 원장/감사 로그가 아직 인메모리라고 표현된 부분이 있다.
- 실제 PostgreSQL 컨테이너 검증을 후속 작업으로 남긴다고 표현된 부분이 있다.
- relay/publisher와 메시지 브로커가 전부 후속 작업이라고 표현된 부분이 있다.
- 인증/인가를 후속 작업으로 남긴다고 표현된 부분이 있다.

현재 상태:

- PostgreSQL profile에서 원장/감사 로그가 DB에 저장된다.
- Testcontainers PostgreSQL 검증과 PostgreSQL scenario CI gate가 있다.
- HTTP outbox broker adapter가 있다.
- Spring Security 기반 운영 API role model이 있다.

판단:

- ADR과 Progress는 최신이며, Wiki draft도 현재 MVP 기준선을 반영하도록 보강했다.
- 단, 과거 progress 문서의 “남은 일”은 당시 시점 기록이므로 소급 수정 대상이 아니다.

### P1. 다음 릴리스 후보 문서

현재 GitHub Release와 `docs/releases/v0.6.0.md`는 존재한다.

하지만 `v0.6.0` 이후 병합된 작업들이 많다.

- outbox relay scheduler
- outbox relay run monitoring
- admin API access audit
- operational log pruning
- relay health metrics/alert
- Spring Security role model
- HTTP outbox broker adapter
- PostgreSQL scenario CI gate

판단:

- 아직 새 버전을 발행하지 않았다면 문제는 아니다.
- `docs/releases/unreleased.md`를 추가해 현재 개발분 검증 기준을 추적한다.
- 실제 tag 발행 전에는 `unreleased`를 버전 릴리스 노트로 승격해야 한다.

### P2. QA Scenarios Wiki 초안

README의 권장 Wiki 구조에는 `QA-Scenarios`가 있다.

현재는 `docs/testing/scenario-test-strategy.md`와 테스트 코드가 QA 시나리오 역할을 일부 수행한다.

판단:

- 코드 검증 기준은 존재한다.
- `wiki-drafts/QA-Scenarios.md`를 추가해 포트폴리오형 설명 문서 gap을 보강했다.

### P2. Architecture Decisions Wiki 초안

README의 권장 Wiki 구조에는 `Architecture-Decisions`가 있다.

현재는 `docs/adr/README.md`가 ADR 목록 역할을 수행한다.

판단:

- source of truth는 충분하다.
- `wiki-drafts/Architecture-Decisions.md`를 추가해 비기술 독자가 읽기 쉬운 결정 지도를 보강했다.

### P2. MCP and Skills Wiki 초안

README의 권장 Wiki 구조에는 `MCP-and-Skills`가 있다.

현재 `skills/` 동기화와 Codex 스킬 사용 흔적은 있지만 Wiki 초안은 없다.

판단:

- `wiki-drafts/MCP-and-Skills.md`를 추가해 MCP/스킬 사용 원칙과 후보를 보강했다.
- 기능 검증에는 영향이 낮다.
- 하네스 운영 포트폴리오 관점에서는 보강 가치가 있다.

### P3. `.dev/rules` 기반 자동 체크 규칙

`check` 스킬은 `.dev/rules` 디렉터리를 기준으로 변경 누락을 자동 검증하도록 설계되어 있다.

현재 repo에는 `.dev/rules`가 없다.

판단:

- 지금은 문서와 PR 템플릿 중심으로 충분히 운영 가능하다.
- 반복 누락이 늘어나면 `.dev/rules`에 문서 동기화 규칙을 추가하는 것이 좋다.

## 권장 후속 작업

1. Wiki draft 게시 또는 동기화
   - `wiki-drafts/` 초안을 실제 GitHub Wiki에 반영
2. 다음 릴리스 후보 문서 유지
   - `docs/releases/unreleased.md`를 실제 tag 발행 전 버전 릴리스 노트로 승격
3. Release Notes Wiki 초안 추가
   - 실제 tag 발행 시 release note를 Wiki 요약과 연결
4. 자동 점검 규칙 도입 검토
   - `.dev/rules/documentation-sync.md`
   - `.dev/rules/testing-gates.md`

## 최종 판정

- 기능 검증 문서: PASS
- ADR/Progress 추적: PASS
- Issue/PR/CI 연결: PASS
- 릴리스 최신 추적: PASS
- Wiki 최신성: PASS
- 자동 누락 점검 규칙: OPTIONAL
