# ADR-0036: PostgreSQL Scenario Testcontainers CI Gate

## 상태

Accepted

## 맥락

기존 테스트 체계는 세 층으로 나뉘어 있다.

- `test`: 도메인, application, API, repository 중심의 빠른 회귀 테스트
- `scenarioTest`: 인메모리 기본 profile 기반 대표 사용자/운영 흐름 테스트
- Testcontainers repository test: 실제 PostgreSQL SQL, row lock, transaction 동작 검증

이 구조는 빠른 개발에는 적합하지만, Spring Boot `postgres` profile 전체가 실제 PostgreSQL 컨테이너에서 API → service → JDBC repository → Flyway migration → outbox relay까지 이어지는지는 별도 CI 신호로 보이지 않는다.

금융/핀테크 학습 프로젝트에서는 H2 호환 모드나 인메모리 흐름만으로 릴리스 판단을 하면 실제 DB profile의 정합성 리스크를 놓칠 수 있다. 특히 잔액, 원장, 감사 로그, step log, outbox는 하나의 업무 흐름 안에서 함께 검증되어야 한다.

Spring Boot 4에서는 Flyway auto-configuration이 `spring-boot-flyway` 모듈에 있다. 따라서 Flyway core dependency만으로는 runtime profile의 migration 자동 적용을 보장하지 않는다. PostgreSQL profile을 실제 실행 가능한 profile로 유지하려면 해당 auto-configuration module을 명시적으로 포함해야 한다.

## 선택지

### 선택지 A: 기존 repository Testcontainers 테스트만 유지한다

장점:

- 추가 CI 시간이 거의 없다.
- 실패 범위가 JDBC repository로 좁아 디버깅이 쉽다.
- 로컬 Docker 의존성을 더 늘리지 않는다.

단점:

- Spring `postgres` profile bean wiring을 전체 흐름으로 검증하지 못한다.
- API validation, service orchestration, Flyway migration, outbox relay 연결을 한 번에 보지 못한다.
- CI job 이름만 보고 PostgreSQL 대표 흐름 통과 여부를 판단하기 어렵다.

### 선택지 B: 기존 `scenarioTest`에 PostgreSQL 시나리오를 포함한다

장점:

- 시나리오 실행 명령이 하나로 유지된다.
- 대표 흐름 테스트 위치가 단순하다.

단점:

- 일반 시나리오 테스트가 Docker/Testcontainers 의존성을 갖는다.
- Docker가 꺼진 로컬 환경에서 기본 시나리오 실행 경험이 나빠진다.
- 인메모리 대표 흐름 실패와 PostgreSQL 인프라 실패가 같은 job에 섞인다.

### 선택지 C: 별도 `postgresScenarioTest` task와 CI job을 둔다

장점:

- PostgreSQL 운영 유사성 게이트가 CI에서 독립적으로 보인다.
- 기존 `test`와 `scenarioTest`의 빠른 피드백 속도를 유지한다.
- Docker/Testcontainers 실패와 도메인 시나리오 실패를 분리할 수 있다.
- 릴리스 검증 시 PostgreSQL profile 통과 여부를 명확히 체크할 수 있다.

단점:

- CI job이 하나 늘어 실행 시간이 증가한다.
- 로컬에서 실행하려면 Docker daemon이 필요하다.
- Spring Boot 컨텍스트와 PostgreSQL 컨테이너를 함께 띄우므로 실패 로그가 repository 단위 테스트보다 길다.

## 결정

별도 `postgresScenarioTest` Gradle task와 GitHub Actions `PostgreSQL Scenario Test` job을 둔다.

테스트 경계는 다음과 같다.

| 게이트 | 명령 | 역할 |
| --- | --- | --- |
| 빠른 회귀 | `./gradlew test` | 단위/API/repository 중심, `scenario`, `postgres-scenario` tag 제외 |
| 기본 시나리오 | `./gradlew scenarioTest` | 인메모리 기본 profile 대표 흐름 |
| PostgreSQL 시나리오 | `./gradlew postgresScenarioTest` | Testcontainers PostgreSQL + Spring `postgres` profile 대표 흐름 |
| 표준 Gradle 검증 | `./gradlew check` | Gradle 기본 verification |

`postgresScenarioTest`는 `postgres-scenario` tag만 실행한다. 이 테스트는 Docker가 필요한 명령이므로 로컬 빠른 게이트에는 포함하지 않는다. CI에서는 별도 job으로 실행해 릴리스 판단 시 PostgreSQL profile의 실제 동작을 확인한다.

Spring Boot `postgres` profile의 Flyway 자동 migration을 검증하기 위해 `spring-boot-flyway` dependency를 추가한다. 테스트에서 migration을 수동 호출하지 않고, application context 초기화 시 runtime profile과 같은 방식으로 migration이 적용되어야 한다.

## 검증 범위

PostgreSQL scenario는 다음 흐름을 한 테스트에서 검증한다.

- Flyway migration으로 실제 PostgreSQL schema와 fixture를 준비한다.
- `postgres` profile에서 `JdbcWalletRepository`가 활성화된다.
- 잔액 조회가 fixture 기준값을 반환한다.
- 충전 생성과 멱등 재시도가 같은 operation을 반환한다.
- 송금 후 출금/입금 지갑 잔액이 갱신된다.
- 원장, 감사 로그, step log, outbox event가 남는다.
- outbox relay가 ready event를 claim/publish하고 `PUBLISHED` 상태로 전이한다.

## 결과

장점:

- PostgreSQL profile의 대표 업무 흐름이 CI에서 독립 게이트로 보인다.
- H2/인메모리 통과와 실제 PostgreSQL 통과를 분리해 판단할 수 있다.
- 향후 MSA 전환 전에도 DB transaction boundary와 outbox 증거 흐름을 반복 검증할 수 있다.

비용:

- CI 실행 시간이 증가한다.
- Docker/Testcontainers 인프라 이슈가 별도 실패 원인이 된다.
- PostgreSQL scenario는 핵심 릴리스 흐름만 유지해야 하며, 모든 edge case를 넣으면 느려진다.

후속 작업:

- PostgreSQL scenario에 운영자 role 기반 manual review/requeue 흐름을 추가할지 별도 검토한다.
- Testcontainers reusable container 또는 build cache 최적화가 필요한지 관찰한다.
- 외부 broker adapter가 추가되면 broker container scenario를 별도 tag로 분리한다.
