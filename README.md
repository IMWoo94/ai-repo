# ai-repo

Java 25와 Spring Boot 기반으로 핀테크 서비스를 학습하고 검증하는 엔지니어링 랩입니다. 목표는 단순한 예제 애플리케이션이 아니라, 금융권 수준의 요구사항을 작은 범위로 쪼개어 설계 근거, 트레이드오프, 코드, 테스트, 리뷰, 릴리스를 함께 남기는 것입니다.

> 이 저장소는 학습용입니다. 실제 금융 거래, 실명 인증, 외부 결제망 연동, 고객 개인정보 처리는 범위에 포함하지 않습니다.

## 진행 배경

핀테크 서비스는 기능 구현보다 정합성, 감사 가능성, 장애 복구, 보안, 운영 절차가 더 중요합니다. 예를 들어 계좌 이체, 간편결제, 포인트 충전, 정산 같은 기능은 화면상으로는 단순하지만 내부적으로는 멱등성, 원장 불변성, 동시성 제어, 한도, 이상거래 탐지, 장애 시 보상 처리까지 고려해야 합니다.

이 레포는 다음 전제를 둡니다.

- 금융 도메인은 코드보다 먼저 정책과 불변식을 정의합니다.
- 모든 기능은 테스트 코드와 함께 작성합니다.
- 설계 선택은 ADR 또는 Wiki 문서로 근거를 남깁니다.
- 코드 리뷰는 구현 품질뿐 아니라 도메인 규칙 위반 여부를 검증합니다.
- 릴리스는 작은 단위로 반복하며 변경 이력을 추적합니다.

## 기술 기준과 근거

| 항목 | 기본 선택 | 근거 | 주요 트레이드오프 |
| --- | --- | --- | --- |
| Java | Java 25 LTS | Oracle은 Java 25를 2025-09-16에 공개했고, Java 릴리스 표에서 Java 25 LTS의 지원 종료를 2033-09로 명시합니다. | 최신 LTS를 통해 장기 학습 기준을 잡을 수 있지만, 일부 라이브러리 호환성은 Java 21보다 늦게 안정화될 수 있습니다. |
| Spring Boot | Spring Boot 4.x 우선 검토 | Spring Boot 4.0.5 문서는 Java 17 이상을 요구하고 Java 26까지 호환된다고 명시합니다. | Spring Framework 7, Jakarta EE 최신 축을 학습할 수 있지만, 생태계 안정성이 더 중요하면 Spring Boot 3.5.x로 낮추는 선택지를 유지합니다. |
| Build | Gradle 9.x 또는 8.14+ | Spring Boot 4.0.5 문서는 Gradle 8.14+ 및 9.x 지원을 명시합니다. | Gradle 9.x는 최신성이 좋고, Gradle 8.14+는 플러그인 호환성이 더 안정적일 수 있습니다. |
| Test | JUnit 5, Spring Boot Test, Testcontainers | 금융 도메인은 DB 트랜잭션과 동시성 검증이 중요하므로 단위 테스트와 통합 테스트를 분리합니다. | 테스트 비용이 증가하지만 회귀와 설계 오류를 조기에 발견합니다. |
| Persistence | PostgreSQL 우선 | 원장, 거래, 감사 로그처럼 정합성이 중요한 데이터에 관계형 모델이 적합합니다. | 빠른 프로토타이핑은 H2가 쉽지만, 운영 유사성은 PostgreSQL이 높습니다. |
| API | REST 우선, 이벤트는 후속 도입 | 초기 학습 단계에서는 요청-응답 계약과 트랜잭션 경계를 명확히 합니다. | 이벤트 기반은 확장성에 유리하지만, 초기에는 복잡도와 관측 비용이 큽니다. |

참고 자료:

- Java 릴리스 지원 표: https://www.java.com/releases/
- Oracle Java 25 발표: https://www.oracle.com/uk/news/announcement/oracle-releases-java-25-2025-09-16/
- Spring Boot 시스템 요구사항: https://docs.spring.io/spring-boot/system-requirements.html

## 목표

1. 금융 서비스 핵심 도메인을 작은 기능 단위로 구현합니다.
2. 기능마다 도메인 규칙, 설계 대안, 트레이드오프, 테스트 전략을 문서화합니다.
3. 코드 개발자 2명, 사용자 개발자, 도메인 전문가, 기획자, QA 역할이 독립적으로 검토하는 하네스를 운영합니다.
4. GitHub Issues, Pull Requests, Wiki, Releases를 사용해 실제 팀 개발 절차를 흉내 냅니다.
5. 각 릴리스마다 기능, 제약, 알려진 리스크, 다음 실험 대상을 기록합니다.

## 초기 도메인 범위

학습 순서는 정합성 난도가 낮은 기능에서 높은 기능으로 확장합니다.

1. 회원과 지갑 계정
2. 잔액 조회와 거래 내역
3. 충전, 출금, 송금
4. 멱등 요청 키와 중복 거래 방지
5. 거래 한도와 상태 전이
6. 원장 모델과 감사 로그
7. 장애 보상, 재처리, 정산 배치
8. 이상거래 탐지 규칙의 기초

## 하네스 엔지니어링 운영 모델

하네스는 역할별 관점을 분리해 같은 기능을 반복 검증하는 작업 방식입니다.

| 역할 | 책임 | 산출물 |
| --- | --- | --- |
| 기획자 | 문제 정의, 사용자 시나리오, 수용 기준 작성 | Issue, 사용자 스토리, acceptance criteria |
| 도메인 전문가 | 금융 규칙, 불변식, 예외 정책 검토 | 도메인 규칙 문서, 용어집, 정책 결정 |
| 사용자 개발자 | 실제 구현 참여, 최종 의사결정 | 코드, 리뷰 의견, 설계 승인 |
| 코드 개발자 A | 구현 또는 리팩터링 담당 | 코드, 단위 테스트, PR 설명 |
| 코드 개발자 B | 대안 구현 검토, 코드 리뷰 담당 | 리뷰 코멘트, 결함 지적, 개선안 |
| QA | 테스트 시나리오, 경계값, 회귀 검증 | QA 체크리스트, 테스트 케이스, 버그 이슈 |
| 릴리스 관리자 | 버전, 변경 이력, 배포 가능 상태 관리 | 태그, GitHub Release, 릴리스 노트 |

### 작업 순서

1. 기획자가 Issue에 문제, 범위, 수용 기준을 작성합니다.
2. 도메인 전문가가 정책, 불변식, 예외 케이스를 보강합니다.
3. 개발자는 기능 브랜치를 생성하고 코드와 테스트를 함께 작성합니다.
4. 코드 개발자 2명이 상호 리뷰를 수행합니다.
5. QA가 수용 기준과 회귀 시나리오를 기준으로 검증합니다.
6. 승인된 변경만 `main`에 병합합니다.
7. 릴리스 대상 변경은 태그와 GitHub Release로 기록합니다.

## 브랜치와 PR 규칙

- 기본 브랜치: `main`
- 기능 브랜치: `feature/<issue-number>-<short-name>`
- 버그 수정 브랜치: `fix/<issue-number>-<short-name>`
- 문서 브랜치: `docs/<issue-number>-<short-name>`
- 릴리스 브랜치: `release/v<major>.<minor>`
- 긴급 수정 브랜치: `hotfix/v<major>.<minor>.<patch>`

PR은 다음 조건을 만족해야 합니다.

- 관련 Issue를 연결합니다.
- 도메인 규칙 변경 여부를 명시합니다.
- 코드와 테스트 코드를 함께 포함합니다.
- 트레이드오프 또는 대안이 있으면 PR 본문이나 ADR에 기록합니다.
- 최소 2개 개발자 관점의 리뷰와 QA 체크를 통과합니다.

GitHub 작업 템플릿:

- [기능 Issue 템플릿](.github/ISSUE_TEMPLATE/feature.yml)
- [버그 Issue 템플릿](.github/ISSUE_TEMPLATE/bug.yml)
- [PR 템플릿](.github/pull_request_template.md)

## 테스트 원칙

테스트는 선택이 아니라 기능 완료 조건입니다.

- 단위 테스트: 도메인 객체, 정책, 상태 전이를 검증합니다.
- 통합 테스트: DB 트랜잭션, Repository, API 경계를 검증합니다.
- 동시성 테스트: 잔액 변경, 중복 요청, 낙관적/비관적 락 전략을 검증합니다.
- 계약 테스트: 외부 시스템을 붙일 경우 요청/응답 계약을 검증합니다.
- 회귀 테스트: 버그 수정 시 실패 재현 테스트를 먼저 추가합니다.

초기 품질 게이트는 다음 명령을 기준으로 잡습니다.

```bash
./gradlew test
./gradlew scenarioTest
./gradlew postgresScenarioTest
./gradlew check
cd frontend && npm run test
cd frontend && npm run build
cd frontend && npm run e2e
```

현재 브랜치에는 Gradle Wrapper와 React/Vite 프론트가 포함되어 있으므로 위 명령을 표준 품질 게이트로 사용합니다. `test`는 단위/API/저장소 중심의 빠른 회귀 게이트이고, `scenarioTest`는 대표 사용자/운영 흐름을 검증하는 시나리오 게이트입니다. `postgresScenarioTest`는 Docker/Testcontainers 기반 실제 PostgreSQL profile 대표 흐름을 검증합니다. `frontend` unit test는 React 상태와 API payload 회귀를 빠르게 검증하고, `frontend` build는 TypeScript/Vite smoke gate이며, `frontend` E2E는 브라우저에서 Vite proxy와 Spring Boot API 연결을 검증합니다.

로컬 테스트 실행 순서와 실패 대응은 [Local Test Guide](docs/testing/local-test-guide.md)를 따릅니다.
시나리오 테스트 추가 기준은 [Scenario Test Strategy](docs/testing/scenario-test-strategy.md)를 따릅니다.
Outbox relay는 publisher port 뒤에 기본 in-memory adapter와 선택형 HTTP broker adapter를 둡니다. 기본 로컬 실행은 `memory`이며, `http` type에서는 실제 HTTP endpoint로 event envelope를 발행하고 contract test로 method/header/body를 검증합니다.

## 로컬 실행

IntelliJ IDEA 기준 설정은 [Local Setup](docs/development/local-setup.md)을 따릅니다.

현재 기능 흐름은 잔액/거래내역 조회, 회원/지갑 계정, 충전/송금, 원장/감사 로그 1차 API를 제공합니다.

- `GET /api/v1/wallets/wallet-001/balance`
- `GET /api/v1/wallets/wallet-001/transactions`
- `POST /api/v1/wallets/wallet-001/charges`
- `POST /api/v1/wallets/wallet-001/transfers`
- `GET /api/v1/wallets/wallet-001/ledger-entries`
- `GET /api/v1/audit-events`
- `GET /api/v1/operations/op-001/step-logs`
- `GET /api/v1/operations/op-001/outbox-events`
- `GET /api/v1/outbox-events/manual-review`
- `POST /api/v1/outbox-events/outbox-001/requeue`
- `GET /api/v1/outbox-events/outbox-001/requeue-audits`
- `GET /api/v1/outbox-relay-runs`
- `GET /api/v1/outbox-relay-runs/health`
- `GET /api/v1/admin-api-access-audits`
- `POST /api/v1/operational-log-pruning-runs`

Outbox 운영 API는 `X-Admin-Token`과 `X-Operator-Id` header를 요구합니다. 로컬 기본 token은 `local-ops-token`이며, 실제 실행에서는 `AI_REPO_OPS_ADMIN_TOKEN` 환경 변수로 override합니다. Requeue audit의 operator는 request body가 아니라 `X-Operator-Id`에서 기록합니다. Relay scheduler 실행 결과는 `/api/v1/outbox-relay-runs`에서 최근 이력으로 조회하고, `/api/v1/outbox-relay-runs/health`에서 health summary와 alert 판정을 조회합니다. 운영 API 접근 성공/실패 이력은 `/api/v1/admin-api-access-audits`에서 조회합니다. 운영 관측 로그 pruning은 `/api/v1/operational-log-pruning-runs`에서 수동 실행합니다.

Outbox relay scheduler는 기본 비활성화입니다. 자동 발행을 로컬에서 확인하려면 `AI_REPO_OUTBOX_RELAY_SCHEDULER_ENABLED=true`를 설정합니다. Batch size와 실행 주기는 `AI_REPO_OUTBOX_RELAY_BATCH_SIZE`, `AI_REPO_OUTBOX_RELAY_INITIAL_DELAY_MS`, `AI_REPO_OUTBOX_RELAY_FIXED_DELAY_MS`로 조정합니다.

Operational log pruning scheduler도 기본 비활성화입니다. 자동 pruning을 로컬에서 확인하려면 `AI_REPO_OPERATIONAL_LOG_PRUNING_SCHEDULER_ENABLED=true`를 설정합니다. Relay run 기본 보존 기간은 30일, admin access audit 기본 보존 기간은 180일입니다.

충전/송금은 `KRW` 단일 통화와 멱등키를 사용합니다. 원장/감사 로그는 Issue #7 기준으로 확장 중이며, 1차 범위에서는 인메모리 조회 API로 검증합니다.

기본 실행은 인메모리 저장소를 사용합니다. PostgreSQL 저장소는 `postgres` 프로필에서 활성화합니다.

로컬 PostgreSQL은 `compose.yml`의 `postgres` 서비스로 실행할 수 있습니다.

`postgres` 프로필의 스키마 기준은 Flyway migration입니다. 초기 migration은 `src/main/resources/db/migration`에 두며, 기존 `src/main/resources/db/postgresql` SQL 파일은 H2 테스트와 수동 비교를 위해 일시적으로 유지합니다.

React 사용자 화면과 운영자 manual review 콘솔은 [React User Frontend](docs/frontend/react-user-frontend.md)를 따릅니다. 백엔드는 `./gradlew bootRun`으로 실행하고, 프론트는 `frontend`에서 `npm install`, `npm run dev`로 실행합니다. 로컬 검증은 [Local Test Guide](docs/testing/local-test-guide.md)를 기준으로 합니다.

## 문서화 전략

README는 저장소의 계약과 방향을 담고, 상세 문서는 GitHub Wiki를 사용합니다.

중요한 결정의 source of truth는 ADR입니다. README와 Wiki는 ADR을 요약하거나 연결할 수 있지만, 결정 자체를 대체하지 않습니다.

단계별 작업 완료 흔적은 [Progress Reports](docs/progress/README.md)에 남깁니다. ADR이 의사결정 기록이라면, Progress Reports는 각 작업에서 완료된 결과, 검증, 남은 일을 짧게 추적하는 문서입니다.

현재 ADR:

- [ADR-0001: Documentation Source of Truth](docs/adr/0001-documentation-source-of-truth.md)
- [ADR-0002: Test Strategy](docs/adr/0002-test-strategy.md)
- [ADR-0003: Java, Spring Boot, Gradle Baseline](docs/adr/0003-java-spring-boot-gradle-baseline.md)
- [ADR-0004: Gradle Wrapper for Java 25 Runtime](docs/adr/0004-gradle-wrapper-java25-runtime.md)
- [ADR-0005: Member Wallet Account Query Policy](docs/adr/0005-member-wallet-account-query-policy.md)
- [ADR-0006: Charge Transfer Idempotency Policy](docs/adr/0006-charge-transfer-idempotency-policy.md)
- [ADR-0007: Ledger Audit Log Boundary](docs/adr/0007-ledger-audit-log-boundary.md)
- [ADR-0008: PostgreSQL Persistence Profile](docs/adr/0008-postgresql-persistence-profile.md)
- [ADR-0009: PostgreSQL Runtime Verification](docs/adr/0009-postgresql-runtime-verification.md)
- [ADR-0010: Flyway Schema Migrations](docs/adr/0010-flyway-schema-migrations.md)
- [ADR-0011: PostgreSQL Balance Row Locking](docs/adr/0011-postgresql-balance-row-locking.md)
- [ADR-0012: PostgreSQL Lock Timeout Policy](docs/adr/0012-postgresql-lock-timeout-policy.md)
- [ADR-0013: Operation Step Log Before Outbox and Saga](docs/adr/0013-operation-step-log-before-outbox-saga.md)
- [ADR-0014: Transactional Outbox Boundary](docs/adr/0014-transactional-outbox-boundary.md)
- [ADR-0015: Outbox Relay State](docs/adr/0015-outbox-relay-state.md)
- [ADR-0016: Outbox Claiming and Retry Policy](docs/adr/0016-outbox-claiming-retry-policy.md)
- [ADR-0017: Outbox Processing Lease Recovery](docs/adr/0017-outbox-processing-lease-recovery.md)
- [ADR-0018: Outbox Max Attempt and Manual Review](docs/adr/0018-outbox-max-attempt-manual-review.md)
- [ADR-0019: Outbox Manual Review API](docs/adr/0019-outbox-manual-review-api.md)
- [ADR-0020: Outbox Requeue Audit Trail](docs/adr/0020-outbox-requeue-audit-trail.md)
- [ADR-0021: Release Version Baseline](docs/adr/0021-release-version-baseline.md)
- [ADR-0022: Scenario-Based Test Pipeline](docs/adr/0022-scenario-based-test-pipeline.md)
- [ADR-0023: Validation Summary Hardening](docs/adr/0023-validation-summary-hardening.md)
- [ADR-0024: React User Frontend MVP](docs/adr/0024-react-user-frontend-mvp.md)
- [ADR-0025: Frontend E2E Test Pipeline](docs/adr/0025-frontend-e2e-test-pipeline.md)
- [ADR-0026: Frontend Component Test Pipeline](docs/adr/0026-frontend-component-test-pipeline.md)
- [ADR-0027: Outbox Publisher Port](docs/adr/0027-outbox-publisher-port.md)
- [ADR-0028: Admin API Authz Boundary](docs/adr/0028-admin-api-authz.md)
- [ADR-0029: Outbox Relay Scheduler](docs/adr/0029-outbox-relay-scheduler.md)
- [ADR-0030: Outbox Relay Run Monitoring](docs/adr/0030-outbox-relay-run-monitoring.md)
- [ADR-0031: Admin API Access Audit](docs/adr/0031-admin-api-access-audit.md)
- [ADR-0032: Operational Log Pruning](docs/adr/0032-operational-log-pruning.md)
- [ADR-0033: Outbox Relay Health Metrics and Alert](docs/adr/0033-outbox-relay-health-metrics-alert.md)
- [ADR-0034: Spring Security Role Model](docs/adr/0034-spring-security-role-model.md)
- [ADR-0035: HTTP Outbox Broker Adapter](docs/adr/0035-http-outbox-broker-adapter.md)
- [ADR-0036: PostgreSQL Scenario Testcontainers CI Gate](docs/adr/0036-postgresql-scenario-testcontainers-ci.md)

권장 Wiki 구조:

- `Home`: 전체 지도와 현재 릴리스 상태
- `Glossary`: 계정, 지갑, 원장, 거래, 정산 등 용어집
- [`Domain-Rules`](wiki-drafts/Domain-Rules.md): 도메인 불변식과 정책
- `Architecture-Decisions`: ADR 목록과 결정 배경
- [`Development-Workflow`](wiki-drafts/Development-Workflow.md): 브랜치, PR, 리뷰, 테스트 규칙
- [`Harness-Roles`](wiki-drafts/Harness-Roles.md): 하네스 역할별 책임과 체크 기준
- `QA-Scenarios`: QA 관점의 시나리오와 경계값
- `Release-Notes`: 릴리스별 변경 사항과 알려진 리스크
- `MCP-and-Skills`: 사용할 MCP 서버와 커스텀 스킬 목록

ADR은 다음 형식을 사용합니다.

```text
# ADR-0001: 결정 제목

## 상태
Proposed | Accepted | Deprecated | Superseded

## 맥락
문제와 제약 조건

## 선택지
대안 A / B / C

## 결정
선택한 방식

## 결과
장점, 단점, 후속 작업
```

## 릴리스 관리

초기 버전은 `0.x`로 시작합니다.

- `v0.1.0`: 프로젝트 스캐폴딩, 기본 CI, 회원/지갑 계정 뼈대
- `v0.2.0`: 잔액 조회, 거래 내역, 회원/지갑 계정, 충전/송금 1차
- `v0.3.0`: 원장 모델, 감사 로그, PostgreSQL 저장소 후보
- `v0.4.0`: 원장 모델, 감사 로그
- `v0.5.0`: PostgreSQL 스키마 migration, 정산 배치 후보
- `v0.6.0`: outbox 재처리, manual review, requeue 감사 이력, 첫 GitHub Release 기준선

현재 릴리스 노트:

- [v0.6.0 Release Notes](docs/releases/v0.6.0.md)

릴리스 노트에는 반드시 다음을 포함합니다.

- 포함된 기능
- 변경된 도메인 규칙
- 마이그레이션 필요 여부
- 테스트 결과
- 알려진 리스크
- 다음 릴리스 후보

## MCP와 스킬 계획

필요할 때만 연결하고, 연결 목적과 권한 범위를 문서화합니다.

| 후보 | 목적 | 도입 시점 |
| --- | --- | --- |
| GitHub MCP | Issue, PR, Wiki, Release 작업 자동화 | GitHub 작업량이 늘어날 때 |
| PostgreSQL MCP | 스키마 탐색, 쿼리 점검, 테스트 데이터 확인 | DB 모델이 생긴 뒤 |
| 공식 문서 검색 스킬 | Java, Spring, Gradle 변경사항 확인 | 버전 선택 또는 업그레이드 판단 시 |
| 핀테크 도메인 스킬 | 원장, 멱등성, 정산, 한도 정책 체크리스트화 | 도메인 규칙이 반복될 때 |
| QA 시나리오 스킬 | 경계값, 실패 주입, 회귀 케이스 생성 | 기능 테스트가 누적될 때 |

MCP 또는 스킬은 편의를 위한 도구이며, 저장소의 결정 근거를 대체하지 않습니다. 중요한 판단은 Issue, PR, ADR, Wiki에 남깁니다.

### Codex 스킬 동기화

Claude Code용으로 작성된 로컬 스킬은 `skills/skills`와 `skills/user-scope-skills`에 보관하고, Codex에서 사용할 때는 다음 명령으로 사용자 스킬 디렉터리에 변환 설치합니다.

```bash
scripts/sync-codex-skills.py
```

동기화 스크립트는 Codex가 안정적으로 읽을 수 있도록 `SKILL.md` frontmatter를 `name`과 `description`만 남기는 형식으로 변환합니다. `interview` 스킬은 필수 설치 대상으로 검증하며, Codex 내장 스킬과 이름이 충돌하는 `skill-creator`는 기본적으로 설치하지 않습니다.

## 즉시 진행할 첫 작업

1. [잔액/거래내역 첫 Issue 초안](issue-drafts/0001-balance-and-transaction-history.md)을 GitHub Issue로 옮깁니다.
2. Java 25, Spring Boot, Gradle 기준의 프로젝트 스캐폴딩을 생성합니다.
3. 기본 CI에서 `test`와 `check`를 실행하도록 구성합니다.
4. `잔액/거래내역` 조회 API의 코드와 테스트를 작성합니다.
5. 첫 ADR로 `Spring Boot 4.x vs 3.5.x`와 `Gradle 9.x vs 8.14+` 선택을 기록합니다.

## 완료 정의

각 작업은 다음을 모두 만족해야 완료입니다.

- Issue에 수용 기준이 있습니다.
- 구현 코드와 테스트 코드가 함께 있습니다.
- 도메인 규칙이 코드 또는 문서에 반영되어 있습니다.
- 트레이드오프가 필요한 결정은 ADR 또는 PR에 기록되어 있습니다.
- 리뷰와 QA 체크가 완료되어 있습니다.
- 릴리스 대상이면 버전과 릴리스 노트가 갱신되어 있습니다.
