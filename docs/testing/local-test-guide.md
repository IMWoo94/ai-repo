# Local Test Guide

## 목적

이 문서는 로컬에서 백엔드, 프론트, E2E 테스트를 어떤 순서로 실행하고 어떤 결과를 기대해야 하는지 정리한다. IntelliJ IDEA와 터미널을 함께 사용하는 상황을 기준으로 한다.

## 빠른 결론

| 목적 | 명령 | 확인 대상 |
| --- | --- | --- |
| 백엔드 빠른 회귀 | `./gradlew test` | 도메인, application, API, repository 단위 테스트 |
| 대표 업무 흐름 | `./gradlew scenarioTest` | 충전/송금/원장/감사/outbox 시나리오 |
| PostgreSQL 대표 흐름 | `./gradlew postgresScenarioTest` | Testcontainers PostgreSQL, Flyway, postgres profile 시나리오 |
| 백엔드 전체 게이트 | `./gradlew check` | Gradle 표준 검증 |
| 프론트 컴포넌트 테스트 | `cd frontend && npm run test` | React 상태, API payload, 오류 메시지 |
| 프론트 타입/번들 검증 | `cd frontend && npm run build` | TypeScript, Vite build |
| 브라우저 E2E | `cd frontend && npm run e2e` | Spring Boot API, Vite proxy, React 화면 흐름 |

## 권장 실행 순서

작업 전후 전체 확인이 필요하면 다음 순서로 실행한다.

```bash
./gradlew test
./gradlew scenarioTest
./gradlew postgresScenarioTest
./gradlew check
cd frontend
npm run test
npm run build
npm run e2e
```

프론트만 수정했다면 다음 세 명령을 우선 실행한다.

```bash
cd frontend
npm run test
npm run build
npm run e2e
```

백엔드 도메인/API만 수정했다면 다음 세 명령을 우선 실행한다.

```bash
./gradlew test
./gradlew scenarioTest
./gradlew postgresScenarioTest
./gradlew check
```

## 백엔드 테스트

### `./gradlew test`

일반 단위/API/저장소 테스트를 실행한다.

검증 대상:

- 도메인 객체 불변식
- application service 정책
- controller 오류 매핑
- repository 영속화 정책
- rollback, lock timeout, outbox 상태 전이

### `./gradlew scenarioTest`

`scenario` tag가 붙은 대표 업무 흐름 테스트만 실행한다.

현재 대표 흐름:

- 잔액 조회
- 충전
- 멱등 재시도
- 송금
- 잔액/원장/감사/step log/outbox 조회
- manual review와 requeue 감사 흐름

상세 기준은 `scenario-test-strategy.md`를 따른다.

### `./gradlew postgresScenarioTest`

`postgres-scenario` tag가 붙은 PostgreSQL profile 대표 흐름 테스트만 실행한다.

검증 대상:

- Testcontainers PostgreSQL 기동
- Flyway migration 적용
- Spring `postgres` profile의 JDBC repository wiring
- 충전/멱등 재시도/송금 API 흐름
- 잔액/원장/감사/step log/outbox 정합성
- outbox relay publish 상태 전이

이 명령은 Docker daemon이 필요하다. Docker가 꺼져 있으면 실패할 수 있으며, CI의 `PostgreSQL Scenario Test` job과 대응된다.

### `./gradlew check`

Gradle 표준 검증 게이트다. 현재는 `test`를 포함하며, CI의 `Gradle Check` job과 대응된다.

## 프론트 테스트

### 최초 준비

Node/npm이 설치되어 있어야 한다. 의존성 설치는 다음 명령을 사용한다.

```bash
cd frontend
npm install
```

CI와 같은 clean install을 검증하려면 다음 명령을 사용한다.

```bash
npm ci
```

### `npm run test`

Vitest와 Testing Library로 React 컴포넌트 테스트를 실행한다.

검증 대상:

- 초기 지갑 잔액 렌더링
- 충전/송금 금액 입력 상태 독립성
- 충전 요청 payload
- 잔액 부족 송금 오류 메시지

### `npm run build`

TypeScript type check와 Vite production build를 실행한다.

검증 대상:

- React 컴포넌트 타입 오류
- Vite 설정 오류
- 번들 생성 가능 여부

### `npm run e2e`

Playwright로 실제 Chromium 브라우저 테스트를 실행한다.

Playwright 설정은 다음 서버를 자동으로 실행한다.

| 서버 | 실행 방식 | 확인 URL |
| --- | --- | --- |
| Spring Boot | `./gradlew bootRun --no-daemon` | `http://127.0.0.1:8080/api/v1/wallets/wallet-001/balance` |
| Vite | `npm run dev -- --host 127.0.0.1` | `http://127.0.0.1:5173` |

따라서 E2E를 실행하기 위해 백엔드와 프론트를 별도 터미널에서 미리 띄울 필요는 없다.

브라우저를 눈으로 보며 확인하려면 다음 명령을 사용한다.

```bash
npm run e2e:headed
```

## 로컬 E2E 시연 기준

2026-05-02 로컬 시연 명령:

```bash
npm --prefix frontend run e2e
```

통과 기준:

```text
Running 1 test using 1 worker
✓  1 [chromium] › e2e/wallet-flow.spec.ts
1 passed
```

현재 E2E 시나리오는 다음을 검증한다.

- 초기 화면에 `125,000 KRW` 잔액이 표시된다.
- 충전 금액과 송금 금액 입력이 서로 독립적으로 동작한다.
- 충전 실행 후 잔액이 `132,000 KRW`로 증가한다.
- 송금 실행 후 출금 지갑 잔액이 `129,000 KRW`로 감소한다.
- 잔액 부족 송금 시 `INSUFFICIENT_BALANCE` 오류가 표시된다.
- 최근 operation, `LEDGER_RECORDED`, `CHARGE_COMPLETED`, `TRANSFER_COMPLETED` outbox event가 표시된다.

## CI 대응 관계

| CI job | 로컬 명령 |
| --- | --- |
| `Gradle Check` | `./gradlew check` |
| `Scenario Test` | `./gradlew scenarioTest` |
| `PostgreSQL Scenario Test` | `./gradlew postgresScenarioTest` |
| `Frontend Unit Test` | `cd frontend && npm ci && npm run test` |
| `Frontend Build` | `cd frontend && npm ci && npm run build` |
| `Frontend E2E` | `cd frontend && npm ci && npx playwright install --with-deps chromium && npm run e2e` |

## 실패 시 1차 확인

### Outbox 운영 API가 401 또는 403을 반환할 때

Outbox manual review, requeue, requeue audit, relay run 조회, admin access audit 조회, operational log pruning 실행은 운영 API다. 로컬 호출에는 다음 header가 필요하다.

```bash
X-Admin-Token: local-ops-token
X-Operator-Id: local-operator
```

- `401 ADMIN_AUTHENTICATION_REQUIRED`: `X-Admin-Token`이 없거나 값이 다르다.
- `403 ADMIN_AUTHORIZATION_DENIED`: token은 맞지만 `X-Operator-Id`가 없다.
- 운영 API 권한 판단은 Spring Security `ROLE_OPERATOR`, `ROLE_ADMIN` 기반으로 수행한다.
- 실제 로컬 token은 `AI_REPO_OPS_ADMIN_TOKEN`으로 변경할 수 있다.
- relay health summary와 alert 판정은 `GET /api/v1/outbox-relay-runs/health`에서 조회한다.
- 접근 성공/실패 이력은 `GET /api/v1/admin-api-access-audits?limit=10`에서 조회한다.
- 운영 로그 pruning은 `POST /api/v1/operational-log-pruning-runs`로 수동 실행한다.

### Playwright 브라우저가 없을 때

```bash
cd frontend
npx playwright install chromium
```

### 8080 또는 5173 포트가 이미 사용 중일 때

- 이미 실행 중인 Spring Boot 또는 Vite 서버를 종료한다.
- 로컬에서는 Playwright가 기존 서버를 재사용할 수 있으므로, 서버 상태가 오래되었다면 종료 후 다시 실행한다.

### E2E에서 잔액 기대값이 다를 때

- 기본 profile은 인메모리 저장소를 사용한다.
- E2E는 새 Spring Boot 프로세스를 기준으로 `wallet-001` 초기 잔액 `125,000 KRW`를 기대한다.
- 기존 서버를 재사용했다면 이전 수동 조작으로 잔액이 달라졌을 수 있다.

### Gradle wrapper lock 오류가 날 때

- Gradle wrapper가 로컬 Gradle cache lock을 사용하지 못하면 실패할 수 있다.
- 일반 터미널에서 다시 실행하거나, CI와 같이 제한 없는 환경에서 실행한다.

## 테스트 추가 원칙

- 백엔드 정책/불변식은 Gradle 테스트에 둔다.
- 여러 API가 이어지는 대표 업무 흐름은 `scenarioTest`에 둔다.
- 실제 PostgreSQL profile과 Flyway/Testcontainers가 필요한 대표 흐름은 `postgresScenarioTest`에 둔다.
- 브라우저 조작과 프론트-백엔드 연결은 Playwright E2E에 둔다.
- E2E는 느리므로 릴리스 판단에 필요한 핵심 흐름만 유지한다.
