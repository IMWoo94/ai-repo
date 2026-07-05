# 송금 멱등 재시도가 잔액 부족(422)으로 거부됨

## 증상

전액 송금으로 source 잔액이 0이 된 뒤 같은 `idempotencyKey`로 동일 송금을 재시도하면, 저장된 결과를 돌려주지 않고 `InsufficientBalanceException`(422)으로 거부된다. 멱등 재시도가 잔액 상태에 의존해 실패하므로 멱등 계약 위반이다.

근본 원인은 `InMemoryWalletCommandService.transfer`가 잔액 선검사(`sourceBalance.money().lessThan`)를 멱등 조회(`resolveIdempotency`)보다 먼저 수행하는 것이다. `charge`는 이런 선검사가 없어 멱등 조회를 먼저 하므로 두 경로의 순서가 비대칭이었다. 이는 #104(duplicate-key race)와는 별개의 순차 검사 순서 결함이다.

## 결정

- `transfer`에서 source/target wallet 해석 후 fingerprint를 계산하고, `resolveIdempotency`를 먼저 호출해 기록이 있으면 즉시 반환한다.
- 기록이 없을 때만 `findBalance` + `lessThan` 잔액 검사 후 `applyTransfer`를 수행한다.
- fingerprint를 잔액 검사 이전에 계산하므로 같은 키·다른 fingerprint conflict(409) 감지는 유지된다.
- `charge` 로직/시그니처는 건드리지 않는다.

## 검증

- [x] 회귀 테스트: 전액 송금 후 동일 키 재시도 → 저장된 결과(`created()==false`) 반환, 예외 없음
- [x] 기존 conflict/정상/잔액 부족 테스트 유지
- `./gradlew compileTestJava`
- `./gradlew test --tests '*InMemoryWalletCommandServiceTest'`

관련: ADR-0069, progress 0080.
