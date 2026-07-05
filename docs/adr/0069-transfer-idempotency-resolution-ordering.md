# ADR-0069: Transfer Idempotency Resolution Ordering

## 상태

Accepted

## 배경

`InMemoryWalletCommandService.transfer`는 멱등 조회(`resolveIdempotency`)보다 잔액 선검사(`sourceBalance.money().lessThan`)를 먼저 수행했다. 그래서 source 잔액을 전액 송금해 0으로 만든 뒤 같은 `idempotencyKey`로 동일 요청을 재시도하면, 이미 기록된 결과를 돌려주기 전에 `0 < 금액` 조건에 걸려 `InsufficientBalanceException`(422)로 거부됐다.

`charge`는 이런 선검사가 없어 `resolveIdempotency`를 먼저 호출하고 없을 때만 `applyCharge`를 수행한다. 즉 두 경로의 검사 순서가 비대칭이었고, 멱등 재시도가 잔액 상태에 의존해 실패하는 것은 멱등 계약 위반이다.

이 결함은 #104의 duplicate-key race(같은 키 동시 삽입 경합)와는 별개다. #104는 동시성 하에서 두 쓰기가 경합하는 문제이고, 여기서는 단일 스레드에서도 재현되는 순차 검사 순서(ordering) 결함이다.

## 결정

`transfer`에서 멱등 조회를 잔액 검사보다 먼저 수행한다.

| 단계 | 결정 |
| --- | --- |
| wallet 해석 | source/target wallet 해석은 그대로 먼저 수행(소유권·조회 가능 검증 유지) |
| fingerprint 계산 | `sourceWallet.walletId()`/`targetWallet.walletId()`로 잔액 검사 이전에 계산 |
| 멱등 조회 우선 | `resolveIdempotency(idempotencyKey, fingerprint)`가 값이 있으면 즉시 반환(`created()==false`) |
| 잔액 검사 후치 | 기록이 없을 때만 `findBalance` + `lessThan` → `InsufficientBalanceException` 후 `applyTransfer` |
| conflict 유지 | fingerprint가 잔액 검사 이전에 계산되므로, 같은 키·다른 fingerprint → `IdempotencyKeyConflictException`(409) 감지는 그대로 유지 |

`charge` 로직과 시그니처는 건드리지 않는다.

## 트레이드오프

### 장점

- 전액 송금 후 동일 키 재시도가 잔액 상태와 무관하게 저장된 결과를 돌려줘 멱등 계약을 지킨다.
- `charge`와 `transfer`의 검사 순서가 "멱등 조회 우선, 부작용 전 검증 후치"로 일관된다.
- fingerprint를 잔액 검사 이전에 계산해 conflict(409) 감지를 그대로 보존한다.

### 비용

- 잔액이 부족한 신규(미기록) 요청은 여전히 wallet 해석과 멱등 조회를 거친 뒤 422가 나므로, 미세하게 조회 한 번을 더 지난 후 거부된다(부작용은 없음).

## 대안

- 잔액 검사를 유지하되 예외를 잡아 멱등 결과로 복구: 흐름이 복잡해지고 정상 부족 케이스와 재시도 케이스를 예외로 구분해야 해 취약하다.
- `charge`처럼 잔액 선검사를 제거: transfer는 부작용 전 잔액 보장이 필요하므로 검사 자체는 유지하고 순서만 바꾸는 것이 옳다.
