# [Fix] 멱등키를 지갑 스코프로 한정해 회원 간 충돌 차단

## 증상

`wallet_operations` PK가 `idempotency_key` 단일 컬럼(전역 유니크)이라 멱등키가 전역 네임스페이스였다. 서로 다른 회원/지갑이 같은 멱등키 문자열을 쓰면 `findOperation`이 지갑을 넘어 상대 레코드를 찾아 fingerprint 불일치로 409(`IdempotencyKeyConflictException`)를 냈다. 한 회원이 예측 가능한 멱등키만으로 상대 요청을 막을 수 있는 cross-tenant 간섭(409 DoS 가능성)이 있었고, fingerprint까지 같으면 다른 지갑의 operation 결과가 재생됐다.

## 결정

멱등키 유일성 범위를 지갑 스코프로 한정한다.

- `wallet_operations` PK를 `(wallet_id, idempotency_key)` 복합 PK로 전환(`V20`).
- `findOperation(walletId, idempotencyKey)`로 조회 시그니처 변경, JDBC에 `and wallet_id = ?` 추가.
- InMemory 어댑터는 복합 키 저장/조회, 서비스는 charge=대상 지갑, transfer=출금 지갑을 스코프로 전달.
- 지갑 존재 오라클(404/403)은 ADR-0056이 수용한 트레이드오프로 이번 범위에서 변경하지 않음(`WalletAccessPolicy` 불변).

## 검증

- `./gradlew test --tests '*InMemoryWalletCommandServiceTest' --tests '*JdbcWalletRepositoryTest'`
- 서로 다른 두 지갑이 같은 멱등키를 써도 충돌하지 않음, 같은 지갑+같은 키+다른 fingerprint는 여전히 409.
