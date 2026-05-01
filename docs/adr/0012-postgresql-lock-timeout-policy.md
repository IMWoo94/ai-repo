# ADR-0012: PostgreSQL Lock Timeout Policy

## 상태

Accepted

## 맥락

ADR-0011에서 PostgreSQL 잔액 row lock을 도입했다. 이 결정으로 충전과 송금은 `wallet_balances` row를 잠근 뒤 갱신하며, 송금은 locked balance 기준으로 잔액 부족을 재검증한다.

남은 문제는 lock 대기 시간이다. 같은 지갑에 요청이 몰리거나 긴 트랜잭션이 row lock을 오래 잡으면 후속 요청이 사용자 응답 없이 오래 대기할 수 있다. 핀테크 서비스에서는 잔액 부족, 멱등키 충돌, 동시성 경합을 서로 다른 실패로 구분해야 운영 원인 분석과 사용자 재시도 안내가 가능하다.

## 선택지

### 선택지 A: lock timeout 없음

장점:

- 구현이 단순하다.
- lock을 획득할 때까지 기다리므로 일시 경합에서 성공 가능성이 높다.

단점:

- 사용자 응답 시간이 예측 불가능하다.
- 장애 또는 긴 트랜잭션 상황에서 요청 대기가 길어진다.
- 운영 관측 지표에서 경합과 느린 DB 작업을 구분하기 어렵다.

### 선택지 B: 짧은 PostgreSQL lock timeout

장점:

- row lock 경합을 빠르게 실패로 전환한다.
- API가 재시도 가능한 busy 상태를 명확히 표현한다.
- 기존 row-level lock 정책과 직접 연결된다.

단점:

- 일시 경합에서도 실패 응답이 발생할 수 있다.
- timeout 값은 트래픽, SLA, 사용자 경험에 맞춰 조정이 필요하다.

### 선택지 C: 애플리케이션 내부 자동 재시도

장점:

- 사용자가 보는 실패를 줄일 수 있다.
- 짧은 lock 경합은 서버 내부에서 흡수할 수 있다.

단점:

- 재시도 횟수, backoff, 멱등키, 중복 기록 방지 정책이 함께 복잡해진다.
- 초기 학습 단계에서는 장애와 경합을 숨겨 관측 증거가 약해진다.

## 결정

`짧은 PostgreSQL lock timeout`을 선택한다.

초기 구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| timeout 값 | 1초 |
| 설정 위치 | 충전/송금 저장소 트랜잭션 내부, row lock 획득 전 |
| PostgreSQL 설정 | `SET LOCAL lock_timeout = '1000ms'` |
| H2 테스트 호환 | `SET LOCK_TIMEOUT 1000` fallback |
| 도메인 예외 | `WalletConcurrencyException` |
| API 응답 | `409 Conflict`, `WALLET_BALANCE_BUSY` |
| 기록 정책 | timeout 실패 요청은 거래내역, 원장, 감사 로그, 멱등 기록을 남기지 않음 |

## 결과

장점:

- lock 경합이 사용자-facing 오류 코드로 드러난다.
- 잔액 부족(`INSUFFICIENT_BALANCE`)과 지갑 경합(`WALLET_BALANCE_BUSY`)을 분리한다.
- Testcontainers PostgreSQL 테스트로 실제 row lock 보유 상황을 검증한다.

비용:

- 1초 기준은 학습용 초기값이며 운영 SLA에 맞춘 조정이 필요하다.
- 자동 재시도나 `Retry-After` 헤더는 아직 제공하지 않는다.
- 로컬 Docker가 꺼진 환경에서는 PostgreSQL lock timeout 테스트가 스킵될 수 있다.

후속 작업:

- timeout 값을 설정 프로퍼티로 분리할지 결정한다.
- `Retry-After` 헤더와 클라이언트 재시도 안내 정책을 정의한다.
- lock timeout 발생률을 관측 지표로 남길지 검토한다.
