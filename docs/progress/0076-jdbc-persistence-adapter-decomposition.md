# 0076. JDBC Persistence Adapter Decomposition

## 스펙 목표

- Issue #125의 `JdbcWalletRepository` bounded-context 분해를 마무리한다.
- application layer Spring annotation 정책과 JDBC adapter 분해 전략을 ADR로 확정한다.
- 레이어 의존 방향을 ArchUnit 테스트로 고정한다.

## 완료 결과

- `JdbcWalletRepository`를 PostgreSQL profile composite bean으로 유지하고, SQL 구현을 context별 package-private adapter로 분해했다.
  - `JdbcWalletLedgerRepository`
  - `JdbcOutboxRelayRepository`
  - `JdbcOutboxConsumerRepository`
  - `JdbcOperationalAlertRepository`
  - `JdbcAdminApiAccessAuditRepository`
  - `WalletJdbcSupport`
- application layer의 제한적 Spring annotation 허용 정책을 ADR-0063으로 기록했다.
- JDBC persistence adapter 분해 전략을 ADR-0064로 기록했다.
- ArchUnit 의존성 방향 테스트를 추가해 domain/application/api/infra 경계를 고정했다.
- ADR index와 unreleased release note에 변경 내용을 반영했다.

## 검증

- `./gradlew compileJava`
- `./gradlew test --tests '*JdbcWalletRepositoryTest' --tests '*JdbcWalletRepositoryRollbackTest'`
- `./gradlew test`
- `./gradlew postgresScenarioTest`

## 남은 일

- 세부 JDBC adapter별 slice test 분리가 필요하면 별도 후속 작업으로 다룬다.

## 관련 문서

- `docs/adr/0063-application-layer-spring-annotation-policy.md`
- `docs/adr/0064-jdbc-persistence-adapter-decomposition.md`
- `docs/releases/unreleased.md`
