# Idempotency Duplicate-Key Recovery

## 배경

charge/transfer 멱등성은 service의 `findOperation` 체크와 repository의 idempotency key INSERT로 이루어진다. 단일 JVM에서는 service의 `synchronized`가 보호하지만, 멀티-JVM 배포에서는 두 요청이 동시에 `findOperation`을 빈 결과로 보고 apply 단계로 진입할 수 있다. 늦게 INSERT하는 레이스 패자는 `wallet_operations` PK 충돌로 `DuplicateKeyException`을 만나고, 이 예외는 핸들러가 없어 HTTP 500으로 노출된다. 멱등성 API가 재시도 클라이언트에게 500을 주는 것은 핵심 계약 위반이다.

## 목표

- 멱등성 INSERT 충돌을 예외가 아니라 회복으로 처리한다.
- `applyCharge`/`applyTransfer`가 충돌 시 기존 operation을 재조회하여 반환한다.
- 회복된 결과는 `created=false`, 새로 적용된 결과는 `created=true`로 구분한다.
- 같은 key에 다른 fingerprint면 회복하지 않고 `IdempotencyKeyConflictException`을 던진다.
- 두 repository 구현(`Jdbc`, `InMemory`)이 동일한 회복 계약을 따른다.
- 실제 PostgreSQL 동시 charge 레이스로 회귀 테스트한다.

## 완료 조건

- [x] `applyCharge`/`applyTransfer`가 `WalletOperationOutcome(record, created)`를 반환한다.
- [x] 이미 적용된 key로 apply하면 예외 없이 기존 record를 `created=false`로 반환한다.
- [x] 같은 key에 다른 fingerprint면 `IdempotencyKeyConflictException`을 던진다.
- [x] 충돌 시 잔액/거래/원장/감사/아웃박스 부작용이 한 번만 발생한다.
- [x] 실제 PostgreSQL에서 동시 charge 시 정확히 하나만 `created=true`이고 결과 operationId가 동일하다.
- [x] `WalletCommandResult.created`가 outcome의 created를 그대로 전달한다.
- [x] ADR, progress, release, wiki draft가 갱신된다.

## 검증 명령

```bash
./gradlew test --tests '*JdbcWalletRepositoryTest' --tests '*JdbcWalletRepositoryRollbackTest' --tests '*InMemoryWalletCommandServiceTest'
./gradlew postgresScenarioTest --tests '*PostgresContainerWalletRepositoryTest'
scripts/check-dev-rules.sh
```
