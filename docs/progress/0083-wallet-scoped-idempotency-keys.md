# 0083. Wallet-Scoped Idempotency Keys

## 스펙 목표

- Issue #149의 멱등키 전역 네임스페이스로 인한 회원 간 간섭(409 DoS 가능성)을 닫는다.
- `wallet_operations` 멱등키를 지갑 스코프로 한정하고 회귀 테스트로 고정한다.

## 완료 결과

- `wallet_operations` PK를 `(idempotency_key)` → `(wallet_id, idempotency_key)` 복합 PK로 전환했다(`V20` 마이그레이션 + `schema.sql` 반영).
- `WalletCommandRepository.findOperation`을 `(walletId, idempotencyKey)` 시그니처로 바꾸고 JDBC 조회에 `and wallet_id = ?`를 추가했다.
- `InMemoryWalletRepository`의 operation 저장/조회를 `walletId + idempotencyKey` 복합 키로 전환했다.
- `InMemoryWalletCommandService.resolveIdempotency`가 지갑을 전달하도록 하고, charge는 대상 지갑, transfer는 출금(source) 지갑을 스코프로 넘긴다.
- 멱등키 스코핑 결정을 ADR-0072로 기록하고, 지갑 존재 오라클(404/403) 트레이드오프는 ADR-0056 근거로 의도적으로 유지함을 명시했다.

## 검증

- `./gradlew compileTestJava`
- `./gradlew test --tests '*InMemoryWalletCommandServiceTest' --tests '*JdbcWalletRepositoryTest'`
- `AI_REPO_DEV_RULES_BASE=origin/main bash scripts/check-dev-rules.sh`

## 남은 일

- 지갑 존재 오라클(404 vs 403) 열거 축소는 ADR-0056이 수용한 트레이드오프로, 별도 결정 없이는 다루지 않는다.

## 관련 문서

- `docs/adr/0072-wallet-scoped-idempotency-keys.md`
- `docs/adr/0056-enduser-jwt-auth-wallet-ownership.md`
- `docs/releases/unreleased.md`
