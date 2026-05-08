# 0051. Requeue State Transition Atomicity

## 스펙 목표

manual review requeue workflow의 approve/reject/execute 상태 전이를 원자화한다.

동시에 여러 운영 조치가 들어와도 하나의 상태 전이만 성공하고, audit이 중복 생성되지 않아야 한다.

## 완료 결과

- JDBC requeue request 조회에 `for update` row lock을 적용했다.
- execute 단계에서 대상 outbox event도 `for update`로 잠근다.
- approve/reject/execute의 조건부 update count를 검증한다.
- update count가 1이 아니면 `InvalidWalletOperationException`으로 실패한다.
- PostgreSQL Testcontainers에 concurrent execute, concurrent approve/reject 검증을 추가했다.
- `PostgresContainerWalletRepositoryTest`를 `postgres-scenario` tag로 분리해 PostgreSQL gate에서 실행되도록 정리했다.
- PostgreSQL에서 `next_retry_at` CASE update가 timestamp 타입으로 평가되도록 캐스팅을 보강했다.

## 검증

- `./gradlew test --tests "*JdbcWalletRepositoryTest" --tests "*OperationOutboxRelayServiceTest"`
- `./gradlew postgresScenarioTest --tests "*PostgresContainerWalletRepositoryTest"`
- `scripts/check-dev-rules.sh`
- `./gradlew check`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `git diff --check`

## 남은 일

- outbox publish 결과 갱신에도 lease/claim owner 조건을 추가한다.
- 운영자 identity와 workflow role scope를 실제 인증 모델과 연결한다.

## 관련 문서

- `docs/adr/0041-requeue-state-transition-atomicity.md`
- `docs/adr/0038-requeue-approval-workflow.md`
- `docs/adr/0040-direct-requeue-api-deprecation.md`
