# 0080. Transfer Idempotency Resolution Ordering

## 스펙 목표

- Issue #146의 송금 멱등 재시도가 잔액 부족(422)으로 거부되는 결함을 고친다.
- `transfer`의 멱등 조회를 잔액 검사보다 먼저 수행하도록 검사 순서를 바로잡는다.
- `charge`와의 검사 순서 비대칭을 제거하되 `charge` 로직/시그니처는 건드리지 않는다.

## 완료 결과

- `InMemoryWalletCommandService.transfer`에서 fingerprint를 잔액 검사 이전에 계산하고, `resolveIdempotency`를 먼저 호출해 기록이 있으면 즉시 반환하도록 순서를 변경했다.
- 기록이 없을 때만 `findBalance` + `lessThan` 잔액 검사 후 `applyTransfer`를 수행한다.
- fingerprint를 잔액 검사 이전에 계산하므로 같은 키·다른 fingerprint conflict(409) 감지는 그대로 유지된다.
- 회귀 테스트를 추가했다: source 전액을 송금해 잔액을 0으로 만든 뒤 동일 `idempotencyKey`로 재시도하면 저장된 결과(`created()==false`)를 돌려주고 `InsufficientBalanceException`을 던지지 않는다.
- ADR-0069로 검사 순서 결정을 기록하고, #104(duplicate-key race)와 별개의 순서 결함임을 명시했다.

## 검증

- `./gradlew compileTestJava`
- `./gradlew test --tests '*InMemoryWalletCommandServiceTest'`
- `AI_REPO_DEV_RULES_BASE=origin/main bash scripts/check-dev-rules.sh`

## 남은 일

- 없음. JDBC 경로는 멱등 저장 시점의 unique 제약으로 동일 보장을 하며 별도 순서 검사가 없다.

## 관련 문서

- `docs/adr/0069-transfer-idempotency-resolution-ordering.md`
- `docs/releases/unreleased.md`
