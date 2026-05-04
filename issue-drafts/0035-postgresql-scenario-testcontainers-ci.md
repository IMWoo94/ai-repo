# [Feature] PostgreSQL Scenario and Testcontainers CI Gate

## 작업 요약

PostgreSQL profile 기반 대표 업무 흐름을 Testcontainers 시나리오 테스트로 고정하고 GitHub Actions에 별도 게이트를 추가한다.

## 배경과 문제

- 기존 Testcontainers 테스트는 repository 중심으로 실제 PostgreSQL 동작을 검증한다.
- 하지만 Spring Boot `postgres` profile, Flyway migration, API, service, repository, outbox relay가 한 흐름으로 연결되는지 CI에서 별도 이름으로 보이지 않는다.
- 금융/핀테크 학습 프로젝트에서는 H2/인메모리 통과만으로 릴리스 품질을 판단하면 실제 DB 정합성 리스크를 놓칠 수 있다.

## 범위

### 하는 것

- `postgresScenarioTest` 전용 Gradle task를 추가한다.
- Testcontainers PostgreSQL 기반 Spring Boot 시나리오 테스트를 추가한다.
- Spring Boot 4 `spring-boot-flyway` module을 포함해 runtime Flyway auto-configuration을 보장한다.
- GitHub Actions에 `PostgreSQL Scenario Test` job을 추가한다.
- ADR, progress, 테스트 전략 문서를 갱신한다.

### 하지 않는 것

- PostgreSQL을 기본 runtime profile로 전환하지 않는다.
- Kafka/RabbitMQ/SQS 같은 외부 broker는 도입하지 않는다.
- 운영 DB migration 정책을 변경하지 않는다.

## 수용 기준

- [x] `./gradlew postgresScenarioTest`가 실행된다.
- [x] 테스트는 실제 PostgreSQL Testcontainer와 Flyway migration을 사용한다.
- [x] 충전, 멱등 재시도, 송금, 원장/감사/outbox 발행 상태가 한 흐름에서 검증된다.
- [x] 일반 `test`와 기존 `scenarioTest`는 postgres scenario를 중복 실행하지 않는다.
- [x] GitHub Actions에 `PostgreSQL Scenario Test` job이 추가된다.
- [x] ADR/progress/testing 문서가 갱신된다.
- [x] `./gradlew test scenarioTest postgresScenarioTest check`가 통과한다.

## 도메인 규칙과 불변식

- 잔액은 음수가 되지 않아야 한다.
- 충전/송금은 원장, 감사, step log, outbox 증거를 남겨야 한다.
- 멱등 재시도는 거래/원장/감사를 중복 생성하지 않아야 한다.
- PostgreSQL profile에서도 인메모리 대표 시나리오와 같은 결과를 반환해야 한다.

## 하네스 역할 체크

- 기획자: 릴리스 전 대표 DB 흐름을 사용자가 검증 가능해야 한다.
- 도메인 전문가: 실제 DB 트랜잭션 경계에서 금액/원장/outbox 정합성이 유지되어야 한다.
- 코드 개발자 A: 별도 Gradle task와 tag로 실행 경계를 명확히 한다.
- 코드 개발자 B: 기존 빠른 `test`/`scenarioTest`와 중복 실행 비용을 피한다.
- QA: CI job 이름으로 PostgreSQL 흐름 통과 여부를 바로 확인할 수 있어야 한다.
- 릴리스 관리자: Docker/Testcontainers 의존성을 문서화한다.

## 대안과 트레이드오프

### 대안 A: 기존 repository Testcontainers 테스트만 유지

- 장점: 추가 CI 비용이 거의 없다.
- 단점: Spring profile, Flyway, API, service, repository 연결 흐름을 놓칠 수 있다.

### 대안 B: 기존 `scenarioTest`에 PostgreSQL 테스트 포함

- 장점: 시나리오 실행 명령이 하나로 유지된다.
- 단점: 일반 시나리오가 Docker/Testcontainers 의존성을 갖고 로컬 실행 비용이 커진다.

### 선택: 별도 `postgresScenarioTest` task와 CI job

- 장점: 실패 원인이 명확하고, PostgreSQL 운영 유사성 게이트를 CI에서 독립적으로 확인할 수 있다.
- 단점: CI 시간이 증가하고 Docker availability에 의존한다.

## 릴리스 고려사항

- 실행 검증: `./gradlew postgresScenarioTest`
- CI 검증: `PostgreSQL Scenario Test` job
- 마이그레이션: 신규 DB migration 없음
- 알려진 리스크: Docker가 없는 로컬 환경에서는 Testcontainers 기반 테스트가 실행되지 않을 수 있다.
