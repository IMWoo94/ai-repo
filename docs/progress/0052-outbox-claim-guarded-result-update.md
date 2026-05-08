# 0052. Outbox Claim Guarded Result Update

## 스펙 목표

Outbox relay에서 lease가 만료된 뒤 늦게 돌아온 worker가 재claim된 event 상태를 덮어쓰지 못하게 한다.

## 완료 결과

- `markClaimedOutboxEventPublished` repository 계약을 추가했다.
- `markClaimedOutboxEventFailed` repository 계약을 추가했다.
- `publishReadyEvents`는 claim된 event의 `claimedAt`, `leaseExpiresAt`을 결과 갱신 조건으로 사용한다.
- 조건부 update가 1건이 아니면 `InvalidWalletOperationException`으로 실패한다.
- 기존 수동/fixture용 `markPublished`, `markFailed` 경로는 유지했다.
- H2 JDBC repository test와 PostgreSQL Testcontainers test에 stale writer 방지 시나리오를 추가했다.

## 검증

- `./gradlew test --tests "*OperationOutboxRelayServiceTest" --tests "*JdbcWalletRepositoryTest"`
- `./gradlew postgresScenarioTest --tests "*PostgresContainerWalletRepositoryTest"`
- `scripts/check-dev-rules.sh`
- `./gradlew check`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `git diff --check`

## 남은 일

- worker identity 또는 별도 claim token 컬럼 도입을 검토한다.
- broker/consumer idempotency를 별도 단계로 설계한다.

## 관련 문서

- `docs/adr/0042-outbox-claim-guarded-result-update.md`
- `docs/adr/0017-outbox-processing-lease-recovery.md`
