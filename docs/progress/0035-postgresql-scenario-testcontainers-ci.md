# 0035. PostgreSQL Scenario Testcontainers CI

## 스펙 목표

- PostgreSQL profile 전체 흐름을 실제 PostgreSQL 컨테이너에서 검증한다.
- 기존 빠른 `test`와 인메모리 `scenarioTest` 실행 경험을 유지한다.
- CI에서 PostgreSQL 대표 흐름 통과 여부를 별도 job으로 확인한다.

## 완료 결과

- `postgres-scenario` tag 전용 `postgresScenarioTest` Gradle task를 추가했다.
- 일반 `test` task는 `scenario`, `postgres-scenario` tag를 제외하도록 정리했다.
- `PostgresWalletScenarioFlowTest`를 추가해 Testcontainers PostgreSQL과 Spring `postgres` profile을 함께 검증한다.
- Spring Boot 4의 Flyway auto-configuration module인 `spring-boot-flyway`를 추가해 `postgres` profile runtime migration을 복구했다.
- GitHub Actions에 `PostgreSQL Scenario Test` job을 추가했다.
- README, local test guide, scenario test strategy, ADR 목록을 갱신했다.

## 검증

- `./gradlew postgresScenarioTest`
- `./gradlew test scenarioTest postgresScenarioTest check`
- `git diff --check`

## 남은 일

- PostgreSQL scenario에 운영자 manual review/requeue role 흐름을 포함할지 검토한다.
- Testcontainers 실행 시간이 CI 병목이 되는지 관찰한다.
- 외부 broker container scenario는 product-specific broker adapter 단계에서 별도 tag로 분리한다.

## 관련 문서

- `docs/adr/0036-postgresql-scenario-testcontainers-ci.md`
- `docs/testing/scenario-test-strategy.md`
- `docs/testing/local-test-guide.md`
- `issue-drafts/0035-postgresql-scenario-testcontainers-ci.md`
