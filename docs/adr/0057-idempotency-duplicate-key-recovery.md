# ADR-0057: Idempotency Duplicate-Key Recovery

## 상태

Accepted

## 배경

charge/transfer는 멱등성을 위해 두 단계로 동작했다. 먼저 `InMemoryWalletCommandService`가 `findOperation`으로 기존 operation을 조회하고, 없으면 repository의 `applyCharge`/`applyTransfer`가 별도 트랜잭션에서 잔액 변경과 함께 `wallet_operations`에 idempotency key를 INSERT한다. `wallet_operations.idempotency_key`는 PRIMARY KEY이므로 중복 INSERT는 DB에서 거부된다.

이 check-then-act는 단일 JVM에서는 service의 `synchronized`가 직렬화하여 안전하다. 그러나 멀티-JVM(혹은 멀티 인스턴스) 배포에서는 두 요청이 서로 다른 JVM에서 동시에 `findOperation`을 빈 결과로 보고 둘 다 apply 단계로 진입할 수 있다. 이때 늦게 INSERT하는 쪽(레이스 패자)은 `DuplicateKeyException`을 만나고, `executeWithLockTimeout`은 lock timeout만 변환하므로 이 예외는 그대로 전파되어 핸들러가 없는 `DuplicateKeyException` → HTTP 500이 된다.

멱등성 API의 핵심 계약은 "같은 요청을 재시도하면 같은 결과를 안전하게 받는다"이다. 레이스 패자가 500을 받는 것은 이 계약을 위반하며, 클라이언트 재시도 안전성을 깨뜨린다.

## 결정

멱등성 INSERT 충돌을 예외가 아니라 **회복(recovery)** 으로 처리한다. apply 경로에서 `DuplicateKeyException`이 발생하면 같은 트랜잭션 밖에서 `findOperation`으로 이미 커밋된 operation을 재조회하여 그 결과를 반환한다.

| 항목 | 결정 |
| --- | --- |
| 회복 위치 | repository apply 경로 (`JdbcWalletRepository.executeIdempotentOperation`) |
| 충돌 처리 | `DuplicateKeyException` → `findOperation` 재조회 후 기존 record 반환 |
| fingerprint 검증 | 회복된 record의 fingerprint가 다르면 `IdempotencyKeyConflictException` |
| 반환 타입 | `applyCharge`/`applyTransfer`가 `WalletOperationOutcome(record, created)` 반환 |
| created 의미 | 새로 적용하면 `created=true`, 회복하면 `created=false` |
| service 반영 | `WalletCommandResult.created`가 outcome의 created를 그대로 전달 |
| 구현 일관성 | `InMemoryWalletRepository`도 동일하게 회복/충돌 계약을 구현 |

## 트레이드오프

### 장점

- 레이스 패자가 500 대신 멱등 결과(기존 operationId, 동일 잔액)를 받는다.
- 부작용(잔액/거래/원장/감사/아웃박스)은 패자 트랜잭션 롤백으로 정확히 한 번만 발생한다.
- `created` 플래그가 "새로 생성"과 "회복"을 정확히 구분하여 호출자가 의미를 신뢰할 수 있다.
- 회복은 DB 트랜잭션 경계에서 강제되므로 service-level check 우회 여부와 무관하게 안전하다.

### 비용

- `applyCharge`/`applyTransfer` 반환 타입이 `WalletOperationRecord` → `WalletOperationOutcome`으로 바뀌어 두 repository 구현과 service가 함께 변경된다.
- 회복 시 `findOperation` 재조회가 1회 추가되지만, 레이스 충돌이라는 드문 경로에서만 발생한다.

## 검증 기준

- 이미 적용된 idempotency key로 `applyCharge`/`applyTransfer`를 호출하면 예외 없이 기존 record를 `created=false`로 반환한다.
- 같은 key에 다른 fingerprint면 회복하지 않고 `IdempotencyKeyConflictException`을 던진다.
- 실제 PostgreSQL에서 같은 key로 동시 charge하면 정확히 하나는 `created=true`, 하나는 `created=false`이고 부작용은 한 번만 남는다.
- 충돌 시 부분 상태(잔액/원장/감사)가 남지 않는다.

## 후속 작업

- 실제 사용자 인증/소유권(per-user wallet ownership) 도입 시 idempotency fingerprint에 userId를 포함한다.
- 회복 경로 빈도를 관측할 수 있는 metric을 metrics 도입 작업과 함께 검토한다.
