# ADR-0072: Wallet-Scoped Idempotency Keys

## 상태

Accepted

## 배경

`wallet_operations` 테이블의 PK는 `idempotency_key` 단일 컬럼이었다. 즉 멱등키가 전역(global) 네임스페이스였다. 클라이언트가 지갑별로 멱등키를 발급하는데, 서로 다른 회원/지갑이 우연히(또는 의도적으로) 같은 문자열을 멱등키로 쓰면 전역 유니크 제약과 `findOperation` 조회가 지갑을 넘나들며 서로 간섭했다.

구체적 영향:

- 회원 A가 이미 쓴 멱등키를 회원 B가 같은 값으로 충전/송금하면, B의 `findOperation(key)`가 A의 레코드를 찾아 fingerprint 불일치로 `IdempotencyKeyConflictException`(409)이 났다. 즉 한 회원이 상대 회원의 요청을 예측 가능한 멱등키 문자열만으로 막을 수 있는 cross-tenant 간섭(409 DoS 가능성)이 존재했다.
- 반대로 fingerprint까지 우연히 같으면 B가 A의 과거 operation 결과를 그대로 되돌려받을 수 있어, 멱등 재생(replay)이 지갑 경계를 넘었다.

멱등키는 본질적으로 "이 지갑에 대한 이 명령"을 식별하는 스코프이므로, 네임스페이스를 지갑 단위로 한정하는 것이 자연스럽다.

## 결정

멱등키의 유일성 범위를 전역에서 지갑 스코프로 한정한다.

| 항목 | 결정 |
| --- | --- |
| PK 변경 | `wallet_operations` PK를 `(idempotency_key)` → `(wallet_id, idempotency_key)` 복합 PK로 전환 (`V20`) |
| 마이그레이션 | `ALTER TABLE ... DROP CONSTRAINT IF EXISTS wallet_operations_pkey` 후 복합 PK 추가. `wallet_id`는 NOT NULL이고 기존 키가 전역 유니크였으므로 복합 PK도 유니크가 보장돼 기존 데이터에 안전 |
| 조회 시그니처 | `WalletCommandRepository.findOperation(String walletId, String idempotencyKey)`로 변경, JDBC 조회에 `and wallet_id = ?` 추가 |
| InMemory 어댑터 | operation 저장/조회 키를 `walletId + idempotencyKey` 복합으로 전환 |
| 서비스 스코프 | `resolveIdempotency`에 지갑을 전달. charge는 `walletAccount.walletId()`, transfer는 `sourceWallet.walletId()`(출금 지갑)를 스코프로 사용 |

멱등 스코프의 기준 지갑은 명령이 소유권을 강제하는 지갑, 즉 charge의 대상 지갑과 transfer의 출금(source) 지갑이다. 이는 ADR-0056의 소유권 강제 지갑과 동일한 축이다.

## 트레이드오프

### 장점

- 서로 다른 지갑이 같은 멱등키 문자열을 써도 충돌하지 않아 cross-tenant 409 간섭(DoS 가능성)이 닫힌다.
- 멱등 재생(replay)이 지갑 경계 안으로 한정돼 다른 지갑의 operation 결과가 새지 않는다.
- 복합 PK가 여전히 유니크하므로 지갑 내부의 멱등 보장(같은 키 → 같은 결과, 다른 fingerprint → 409)은 그대로 유지된다.

### 비용

- `findOperation` 시그니처가 바뀌어 콜사이트(서비스·JDBC·InMemory·테스트)를 모두 갱신해야 한다.
- 조회 파라미터가 하나 늘어난다(운영 부담은 사실상 없음, PK 인덱스 그대로 사용).

## 대안 / 맥락

- **전역 유니크 유지 + fingerprint에 walletId 포함**: 재생 격리는 되지만 전역 네임스페이스가 그대로라 다른 지갑이 같은 키를 쓰면 여전히 PK 충돌(409)이 난다. cross-tenant 간섭을 닫지 못해 채택하지 않음.
- **지갑 존재 오라클(404 vs 403) 열거 축소는 이번 범위 밖**: 이슈 #149에는 "없는 지갑(404)"과 "남의 지갑(403)"을 구별 가능한 존재 열거 문제도 언급됐으나, 이 트레이드오프는 ADR-0056이 명시적으로 "수용"한 결정이다(비소유자에 403, 404 collapse 안 함). `WalletAccessPolicy`는 변경하지 않고 그 결정을 의도적으로 유지한다. 본 ADR은 멱등키 스코핑만 다룬다.
