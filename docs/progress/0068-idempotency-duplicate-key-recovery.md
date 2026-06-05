# Idempotency Duplicate-Key Recovery

## 스펙 목표

멀티-JVM 레이스에서 멱등성 INSERT 충돌을 만난 패자가 HTTP 500이 아니라 기존 멱등 결과를 받도록, repository apply 경로에서 `DuplicateKeyException`을 회복으로 처리한다.

## 완료 결과

- `JdbcWalletRepository`에 `executeIdempotentOperation`/`recoverIdempotentRecord`를 추가하여 apply 중 `DuplicateKeyException`이 발생하면 `findOperation`으로 기존 operation을 재조회해 반환한다.
- 회복된 record의 fingerprint가 요청과 다르면 `IdempotencyKeyConflictException`을 던지도록 했다.
- `applyCharge`/`applyTransfer` 반환 타입을 `WalletOperationOutcome(record, created)`로 바꿔 "새로 적용"과 "회복"을 구분했다.
- `InMemoryWalletRepository`도 동일한 회복/충돌 계약을 구현했다(기존에는 같은 key를 조용히 덮어썼다).
- `InMemoryWalletCommandService`가 outcome의 `created`를 `WalletCommandResult`에 그대로 전달하여, 회복된 결과가 `created=false`로 보고되도록 했다.
- `JdbcWalletRepositoryTest`에 이미 적용된 key의 회복, 다른 fingerprint 충돌 회귀 테스트를 추가했다.
- `JdbcWalletRepositoryRollbackTest`의 충돌 시나리오 기대 예외를 `DuplicateKeyException`에서 `IdempotencyKeyConflictException`으로 갱신했다(롤백 검증 자체는 유지).
- `PostgresContainerWalletRepositoryTest`에 실제 PostgreSQL 동시 charge 레이스 테스트를 추가하여 정확히 하나는 `created=true`, 하나는 `created=false`이고 부작용이 한 번만 남음을 검증했다.

## 검증

- `./gradlew test`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `scripts/check-dev-rules.sh`

## 남은 일

- 실제 사용자 인증/소유권 도입 시 idempotency fingerprint에 userId를 포함한다.
- 회복 경로 빈도를 관측할 수 있는 metric을 metrics 도입 작업과 함께 검토한다.
