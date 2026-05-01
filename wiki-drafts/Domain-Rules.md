# Domain Rules

이 문서는 핀테크 학습 도메인의 규칙과 불변식을 기록한다.

중요한 도메인 정책이 확정되면 ADR로 승격한다. 이 문서는 사고 과정과 도메인 지식의 축적 공간이다.

## 현재 도메인 진행 순서

1. 잔액과 거래내역
2. 회원과 지갑 계정
3. 충전과 송금
4. 원장과 감사 로그
5. 정산과 재처리
6. 이상거래 탐지

## 용어

| 용어 | 의미 | 상태 |
| --- | --- | --- |
| 회원 | 서비스를 사용하는 주체 | 초안 |
| 지갑 | 회원이 보유한 금액과 거래내역의 기준 단위 | 초안 |
| 잔액 | 특정 지갑의 현재 사용 가능 금액 | 초안 |
| 거래내역 | 잔액에 영향을 준 사건 또는 사용자가 조회할 수 있는 금융 활동 기록 | 초안 |
| 원장 | 감사와 정합성 검증을 위해 보존되는 불변 기록 | DECIDE_LATER |
| 거래 상태 | 거래가 생성, 완료, 실패, 취소 등 어느 단계에 있는지 나타내는 값 | 초안 |

## 잔액 규칙

| 규칙 | 설명 | 검증 방식 |
| --- | --- | --- |
| 잔액은 음수가 될 수 없다 | 어떤 거래 후에도 사용 가능 잔액은 0 이상이어야 한다 | 단위 테스트, 통합 테스트 |
| 잔액은 통화 단위를 가진다 | 금액만 단독으로 두지 않고 통화 또는 단위 정책을 명시한다 | 도메인 테스트 |
| 잔액 조회는 현재 상태를 반환한다 | 조회 시점의 지갑 잔액을 반환한다 | API 통합 테스트 |
| 존재하지 않는 지갑은 실패로 처리한다 | 빈 잔액으로 대체하지 않는다 | API 통합 테스트 |

## 거래내역 규칙

| 규칙 | 설명 | 검증 방식 |
| --- | --- | --- |
| 거래내역은 삭제하지 않는다 | 감사 가능성을 위해 논리적으로 보존한다 | 도메인 정책 검토 |
| 기본 정렬은 최신순이다 | 사용자가 최근 거래를 먼저 확인할 수 있어야 한다 | API 통합 테스트 |
| 빈 거래내역은 정상 결과다 | 거래가 없다는 사실을 빈 목록으로 표현한다 | API 통합 테스트 |
| 거래내역은 상태를 가진다 | 성공, 실패, 취소 등 상태 표현이 필요하다 | 단위 테스트 |
| 거래내역은 금액과 방향을 가진다 | 증가/감소 또는 입금/출금 방향이 명확해야 한다 | 단위 테스트 |

## 조회 기능 예외

| 상황 | 기대 동작 | 비고 |
| --- | --- | --- |
| 존재하지 않는 지갑 | 실패 응답 | 상태 코드와 오류 형식은 API 설계 시 결정 |
| 거래내역 없음 | 빈 목록 반환 | 정상 응답 |
| 잘못된 페이지 요청 | 실패 또는 보정 | 정책 결정 필요 |
| 지원하지 않는 정렬 조건 | 실패 응답 | 정책 결정 필요 |
| 인증 없는 접근 | 아직 범위 밖 | 인증/인가 ADR 이후 결정 |

## 상태 후보

거래 상태는 아직 확정하지 않는다. 초기 후보는 다음과 같다.

| 상태 | 의미 | 비고 |
| --- | --- | --- |
| `PENDING` | 거래 요청이 생성되었지만 완료 전 | 충전/송금에서 필요 가능 |
| `COMPLETED` | 거래가 성공적으로 반영됨 | 조회 기능에서 우선 사용 가능 |
| `FAILED` | 거래가 실패함 | 실패 이력 보존 필요 |
| `CANCELED` | 요청이 취소됨 | 정책 결정 필요 |

## 첫 기능 흐름: 잔액/거래내역

### 정상 흐름

1. 사용자가 지갑의 잔액을 조회한다.
2. 시스템은 지갑이 존재하는지 확인한다.
3. 시스템은 현재 잔액과 통화 정보를 반환한다.
4. 사용자가 거래내역을 조회한다.
5. 시스템은 최신순 거래 목록과 페이지 정보를 반환한다.

### 최소 응답 정보 후보

잔액 조회:

- 지갑 식별자
- 잔액
- 통화
- 조회 시각

거래내역 조회:

- 거래 식별자
- 거래 일시
- 거래 유형
- 거래 상태
- 거래 금액
- 통화
- 잔액 변화 방향
- 설명

## 테스트 관점

| 테스트 | 목적 |
| --- | --- |
| 잔액 음수 방지 단위 테스트 | 도메인 불변식 검증 |
| 존재하지 않는 지갑 조회 통합 테스트 | 실패 응답 검증 |
| 거래내역 빈 목록 통합 테스트 | 정상 빈 결과 검증 |
| 거래내역 최신순 정렬 테스트 | 기본 조회 정책 검증 |
| 잘못된 페이지 요청 테스트 | 경계값 검증 |

## DECIDE_LATER

- 잔액을 `availableBalance`와 `ledgerBalance`로 나눌지 여부.
- 거래내역과 원장을 같은 모델로 볼지 분리할지 여부.
- 거래 상태 목록을 어디까지 둘지 여부.
- 통화 정책을 원화 단일로 둘지 다중 통화 가능성을 열지 여부.
- 인증/인가 도입 전 조회 권한을 어떻게 표현할지 여부.

## 회원/지갑 계정 규칙 후보

| 규칙 | 설명 | 상태 |
| --- | --- | --- |
| 회원은 0개 이상의 지갑을 가질 수 있다 | 장기적으로 복수 지갑을 열어둔다 | 초안 |
| 초기 API는 기본 지갑 1개 흐름을 사용한다 | 첫 기능 범위를 줄인다 | 초안 |
| 지갑은 소유 회원 식별자를 가진다 | 조회와 돈 이동의 주체를 명확히 한다 | 초안 |
| 지갑 식별자는 전역에서 유일하다 | API 조회 기준이 된다 | 초안 |
| 지갑은 상태를 가진다 | 활성/정지/해지 등 정책 분리를 준비한다 | 초안 |
| 조회는 회원과 지갑이 모두 활성 상태여야 한다 | 비활성 계정의 잔액/거래내역 노출을 막는다 | ADR-0005 |

### 지갑 상태 후보

| 상태 | 의미 | 조회 정책 |
| --- | --- | --- |
| `ACTIVE` | 정상 사용 가능한 지갑 | 잔액/거래내역 조회 가능 |
| `SUSPENDED` | 임시 제한된 지갑 | 정책 결정 필요 |
| `CLOSED` | 닫힌 지갑 | 거래내역 보존 여부 결정 필요 |

### 회원 상태 후보

| 상태 | 의미 | 조회 정책 |
| --- | --- | --- |
| `ACTIVE` | 정상 사용 가능한 회원 | 활성 지갑의 잔액/거래내역 조회 가능 |
| `SUSPENDED` | 임시 제한된 회원 | 잔액/거래내역 조회 불가 |
| `CLOSED` | 탈퇴 또는 종료된 회원 | 보존 정책은 후속 결정 필요 |

### Issue #3 구현 기준

- 회원과 지갑 계정은 코드에서 별도 도메인 모델로 표현한다.
- 지갑 계정은 소유 회원 식별자를 반드시 가진다.
- 초기 조회 API는 `ACTIVE` 회원의 `ACTIVE` 지갑만 조회 가능하게 한다.
- `SUSPENDED`, `CLOSED` 회원/지갑의 세부 보존·열람 정책은 이후 도메인 결정으로 남긴다.
- 기준 결정은 ADR-0005를 따른다.

## 충전/송금 1차 규칙

| 규칙 | 설명 | 상태 |
| --- | --- | --- |
| 충전 금액은 0보다 커야 한다 | 0원 또는 음수 충전은 잔액 변경으로 인정하지 않는다 | ADR-0006 |
| 송금 금액은 0보다 커야 한다 | 0원 또는 음수 송금은 실패한다 | ADR-0006 |
| 송금 후 출금 지갑 잔액은 음수가 될 수 없다 | 잔액 부족 송금은 실패한다 | ADR-0006 |
| 충전/송금은 원화만 지원한다 | 초기 학습 범위는 `KRW` 단일 통화로 제한한다 | ADR-0006 |
| 충전/송금은 활성 회원의 활성 지갑에서만 가능하다 | 조회 정책과 같은 계정 상태 검증을 사용한다 | ADR-0005, ADR-0006 |
| 모든 잔액 변경은 거래내역을 남긴다 | 사용자가 조회할 수 있는 변경 기록을 보존한다 | 초안 |
| 멱등키는 필수다 | 같은 요청 재시도는 잔액을 한 번만 변경한다 | ADR-0006 |

### 충전/송금 실패 정책

| 상황 | 기대 동작 | 오류 코드 |
| --- | --- | --- |
| 금액이 0 이하 | 실패 응답 | `INVALID_WALLET_OPERATION` |
| 지원하지 않는 통화 | 실패 응답 | `INVALID_WALLET_OPERATION` |
| 잔액 부족 | 실패 응답 | `INSUFFICIENT_BALANCE` |
| 같은 멱등키와 다른 요청 | 실패 응답 | `IDEMPOTENCY_KEY_CONFLICT` |
| 비활성 회원 또는 지갑 | 실패 응답 | `WALLET_NOT_QUERYABLE` |

### Issue #5 구현 기준

- `POST /api/v1/wallets/{walletId}/charges`로 충전한다.
- `POST /api/v1/wallets/{walletId}/transfers`로 송금한다.
- 신규 성공 요청은 `201 Created`, 같은 멱등키 재시도는 `200 OK`를 반환한다.
- 인메모리 저장소는 프로세스 재시작 시 초기화되므로 릴리스 리스크에 명시한다.
- 기준 결정은 ADR-0006을 따른다.

## 원장/감사 로그 1차 규칙

| 기록 | 목적 | 책임 |
| --- | --- | --- |
| 거래내역 | 사용자 조회 | 사용자가 이해할 수 있는 금융 활동 기록 |
| 원장 엔트리 | 잔액 정합성 검증 | 잔액 변경과 변경 후 잔액의 시스템 기록 |
| 감사 이벤트 | 명령 처리 추적 | 어떤 명령이 성공적으로 처리되었는지 남기는 운영 기록 |

| 규칙 | 설명 | 상태 |
| --- | --- | --- |
| 성공한 충전은 원장 엔트리를 남긴다 | 충전 후 잔액을 검증할 수 있어야 한다 | ADR-0007 |
| 성공한 송금은 양쪽 지갑에 원장 엔트리를 남긴다 | 출금과 입금 지갑의 잔액 변경을 각각 검증한다 | ADR-0007 |
| 원장 엔트리는 변경 후 잔액을 가진다 | 잔액 재계산과 검증의 기준이 된다 | ADR-0007 |
| 성공한 충전/송금은 감사 이벤트를 남긴다 | 명령 처리 추적을 거래내역과 분리한다 | ADR-0007 |
| 멱등 재시도는 원장/감사 기록을 중복 생성하지 않는다 | 같은 요청은 하나의 결과로 수렴해야 한다 | ADR-0006, ADR-0007 |

### Issue #7 구현 기준

- `GET /api/v1/wallets/{walletId}/ledger-entries`로 지갑별 원장을 조회한다.
- `GET /api/v1/audit-events`로 감사 이벤트를 조회한다.
- 원장과 감사 로그는 아직 인메모리이며 프로세스 재시작 시 초기화된다.
- 원장 append-only DB 제약은 PostgreSQL 도입 시 별도 결정한다.
- 기준 결정은 ADR-0007을 따른다.

## PostgreSQL 영속화 1차 규칙

| 규칙 | 설명 | 상태 |
| --- | --- | --- |
| 기본 프로필은 인메모리 저장소를 사용한다 | 로컬 실행과 CI 안정성을 유지한다 | ADR-0008 |
| `postgres` 프로필은 JDBC 저장소를 사용한다 | PostgreSQL 스키마와 저장소 매핑을 검증한다 | ADR-0008 |
| 멱등키는 DB에 저장한다 | 재시도 중복 반영 방지를 프로세스 수명 밖으로 옮긴다 | ADR-0008 |
| 원장과 감사 로그는 DB에 저장한다 | 잔액 변경 검증과 명령 처리 추적 기록을 보존한다 | ADR-0008 |
| 충전/송금 성공 저장은 트랜잭션으로 묶는다 | 잔액, 거래내역, 원장, 감사 로그, 멱등키 기록의 부분 반영을 막는다 | ADR-0008 |

### Issue #9 구현 기준

- `src/main/resources/db/postgresql/schema.sql`에 1차 스키마를 둔다.
- `src/main/resources/db/postgresql/fixtures.sql`에 샘플 데이터를 둔다.
- `JdbcWalletRepository`는 `postgres` 프로필에서만 활성화한다.
- H2 PostgreSQL mode 테스트로 JDBC 매핑과 멱등성 동작을 검증한다.
- 실제 PostgreSQL 컨테이너 검증은 후속 Testcontainers 또는 Docker Compose 작업으로 남긴다.

## PostgreSQL 런타임 검증 규칙

| 검증 방식 | 목적 | 상태 |
| --- | --- | --- |
| H2 PostgreSQL mode | 빠른 JDBC 매핑 검증 | ADR-0008 |
| Testcontainers PostgreSQL | 실제 PostgreSQL 엔진 호환성 검증 | ADR-0009 |
| Docker Compose | 개발자 로컬 `postgres` 프로필 수동 검증 | ADR-0009 |

### Issue #11 구현 기준

- Testcontainers로 실제 PostgreSQL 저장소 테스트를 실행한다.
- 충전, 송금, 멱등 재시도, 원장/감사 로그 저장을 실제 PostgreSQL에서 검증한다.
- 로컬 PostgreSQL은 `docker compose up -d postgres`로 실행한다.
- Docker가 없는 로컬 환경에서는 Testcontainers 테스트가 스킵될 수 있음을 문서화한다.

## Flyway 스키마 버전 관리 규칙

| 규칙 | 설명 | 상태 |
| --- | --- | --- |
| PostgreSQL 프로필은 Flyway migration을 기준으로 한다 | 운영 유사 실행 경로에서 Spring SQL init 대신 migration 이력을 사용한다 | ADR-0010 |
| 적용된 migration은 수정하지 않는다 | 이미 적용된 DB와 코드 기준의 불일치를 막는다 | ADR-0010 |
| 스키마 변경은 새 migration으로 추가한다 | PR과 릴리스에서 DB 변경 여부를 추적한다 | ADR-0010 |
| 기존 SQL 파일은 일시 유지한다 | H2 빠른 테스트와 수동 비교 경로를 보존한다 | ADR-0010 |
| 샘플 데이터는 운영 정책이 아니다 | `V2` fixture는 학습/검증 seed이며 실서비스 데이터 정책과 분리한다 | 초안 |

### Issue #13 구현 기준

- `postgres` 프로필은 `spring.sql.init`이 아니라 Flyway를 사용한다.
- `V1__create_wallet_schema.sql`은 회원, 지갑, 잔액, 거래내역, 멱등키, 원장, 감사 로그 스키마를 생성한다.
- `V2__seed_wallet_fixture.sql`은 학습용 회원, 지갑, 잔액, 거래내역 샘플 데이터를 생성한다.
- Testcontainers PostgreSQL 테스트는 Flyway migration으로 DB를 준비한다.
- 기준 결정은 ADR-0010을 따른다.

## PostgreSQL 잔액 동시성 규칙

| 규칙 | 설명 | 상태 |
| --- | --- | --- |
| 잔액 갱신은 DB row lock 안에서 수행한다 | 여러 애플리케이션 인스턴스가 같은 지갑을 갱신해도 잔액 불변식을 DB가 보호한다 | ADR-0011 |
| 송금은 두 지갑을 결정적 순서로 잠근다 | 출금/입금 지갑을 서로 다른 순서로 잠가 deadlock이 생기는 위험을 줄인다 | ADR-0011 |
| 송금 잔액 부족은 트랜잭션 내부에서 재검증한다 | 서비스 사전 검증이 통과해도 lock 대기 후 잔액이 달라질 수 있다 | ADR-0011 |
| 실패한 송금은 기록을 남기지 않는다 | 1차 범위에서는 성공한 송금만 거래내역, 원장, 감사 로그, 멱등 기록을 남긴다 | ADR-0011 |

### Issue #15 구현 기준

- 충전은 대상 지갑의 `wallet_balances` row를 `SELECT ... FOR UPDATE`로 잠근 뒤 갱신한다.
- 송금은 출금/입금 지갑의 `wallet_balances` row를 결정적 순서로 잠근 뒤 갱신한다.
- 동시 송금으로 잔액을 초과하면 하나만 성공하고 나머지는 `InsufficientBalanceException`으로 실패한다.
- 성공한 송금만 원장/감사 로그/멱등 기록을 생성한다.
- 기준 결정은 ADR-0011을 따른다.

## PostgreSQL Lock Timeout 규칙

| 규칙 | 설명 | 상태 |
| --- | --- | --- |
| 잔액 row lock 대기는 1초로 제한한다 | 경합 요청이 사용자 응답 없이 오래 대기하지 않도록 한다 | ADR-0012 |
| lock timeout은 잔액 부족과 구분한다 | 잔액 부족은 돈이 모자란 상태이고, busy는 재시도 가능한 동시성 경합이다 | ADR-0012 |
| busy 실패는 기록을 남기지 않는다 | lock을 얻지 못한 요청은 잔액 변경, 거래내역, 원장, 감사 로그, 멱등 기록을 만들지 않는다 | ADR-0012 |
| API는 `WALLET_BALANCE_BUSY`를 반환한다 | 클라이언트가 재시도 가능한 상태를 구분할 수 있게 한다 | ADR-0012 |

### Issue #17 구현 기준

- 충전/송금 저장소 트랜잭션은 row lock 획득 전에 lock timeout을 설정한다.
- PostgreSQL lock timeout은 `WalletConcurrencyException`으로 변환한다.
- API는 `WalletConcurrencyException`을 `409 Conflict`, `WALLET_BALANCE_BUSY`로 반환한다.
- Testcontainers PostgreSQL 테스트는 row lock 보유 상황에서 timeout 변환과 기록 미생성을 검증한다.
- 기준 결정은 ADR-0012를 따른다.

## 논리적 트랜잭션 단계 로그 규칙

| 규칙 | 설명 | 상태 |
| --- | --- | --- |
| step log는 처리 과정 관측 기록이다 | 사용자 거래내역, 원장, 감사 이벤트와 책임을 분리한다 | ADR-0013 |
| 성공한 명령은 단계별 완료 기록을 남긴다 | 모놀리스 내부에서도 논리적 트랜잭션 진행 순서를 확인한다 | ADR-0013 |
| 멱등 재시도는 step log를 중복 생성하지 않는다 | 재시도는 기존 operation 결과로 수렴한다 | ADR-0013 |
| 실패 요청은 step log를 남기지 않는다 | 1차 범위에서는 성공 단계의 완료 증거만 기록한다 | ADR-0013 |
| 단계 경계는 MSA 전환 후보가 된다 | 추후 Saga step 또는 Outbox event 후보를 식별한다 | ADR-0013 |

### Issue #19 구현 기준

- 충전/송금 성공 시 `BALANCE_LOCKED`, `BALANCE_UPDATED`, `TRANSACTION_RECORDED`, `LEDGER_RECORDED`, `AUDIT_RECORDED`, `IDEMPOTENCY_RECORDED` 순서로 기록한다.
- `GET /api/v1/operations/{operationId}/step-logs`로 단계 로그를 조회한다.
- PostgreSQL은 Flyway `V3__create_operation_step_logs.sql`로 테이블을 추가한다.
- H2 빠른 저장소 테스트용 `schema.sql`에도 같은 테이블을 반영한다.
- 기준 결정은 ADR-0013을 따른다.

## Transactional Outbox 규칙

| 규칙 | 설명 | 상태 |
| --- | --- | --- |
| outbox event는 외부 반응 후보이다 | 알림, 정산, 리포팅, 이상거래 탐지 같은 서비스로 전달할 integration event 경계다 | ADR-0014 |
| outbox event는 돈 이동 결과와 같은 트랜잭션에 저장한다 | 성공 결과와 이벤트 적재의 이중 쓰기 문제를 줄인다 | ADR-0014 |
| step log와 outbox event는 다르다 | step log는 내부 처리 과정 관측 기록이고 outbox는 외부 전달 후보이다 | ADR-0014 |
| 멱등 재시도는 outbox event를 중복 생성하지 않는다 | 재시도는 기존 operation 결과로 수렴한다 | ADR-0014 |
| 실패 요청은 outbox event를 남기지 않는다 | 1차 범위에서는 성공한 operation만 외부 반응 후보가 된다 | ADR-0014 |

### Issue #21 구현 기준

- 충전/송금 성공 시 `operation_outbox_events`에 `PENDING` event를 1건 저장한다.
- `GET /api/v1/operations/{operationId}/outbox-events`로 outbox event를 조회한다.
- PostgreSQL은 Flyway `V4__create_operation_outbox_events.sql`로 테이블을 추가한다.
- H2 빠른 저장소 테스트용 `schema.sql`에도 같은 테이블을 반영한다.
- relay/publisher와 메시지 브로커는 후속 작업으로 남긴다.
- 기준 결정은 ADR-0014를 따른다.

## Outbox Relay 상태 규칙

| 규칙 | 설명 | 상태 |
| --- | --- | --- |
| 신규 outbox event는 `PENDING`이다 | 아직 외부 브로커로 발행되지 않은 상태이다 | ADR-0015 |
| 발행 성공 event는 `PUBLISHED`가 된다 | relay가 발행 완료 시간을 기록한다 | ADR-0015 |
| 발행 실패 event는 `FAILED`가 된다 | 실패 횟수와 마지막 오류를 기록한다 | ADR-0015 |
| relay 상태 전이는 돈 이동 결과를 바꾸지 않는다 | 지갑 잔액, 원장, 감사 기록과 outbox 발행 상태를 분리한다 | ADR-0015 |
| 병렬 claiming은 `PROCESSING` 상태로 분리한다 | claim된 event는 다른 relay가 다시 가져가지 않도록 처리 중 상태로 전이한다 | ADR-0016 |
| 실패 event는 재시도 예정 시각을 가진다 | `nextRetryAt` 이전에는 retry claim 대상이 아니다 | ADR-0016 |
| 처리 중 event는 lease를 가진다 | `PROCESSING` event는 `claimedAt`, `leaseExpiresAt`으로 회수 가능 시각을 가진다 | ADR-0017 |
| lease 만료 event는 재claim 가능하다 | worker crash로 고착된 event를 다시 처리 대상으로 가져올 수 있다 | ADR-0017 |
| 반복 실패 event는 수동 확인 상태로 격리한다 | 3회 실패한 event는 `MANUAL_REVIEW`가 되고 자동 claim 대상에서 제외된다 | ADR-0018 |

### Issue #23 구현 기준

- pending outbox event를 제한 개수만큼 조회한다.
- 발행 성공 시 `PUBLISHED`, `publishedAt`을 기록한다.
- 발행 실패 시 `FAILED`, `attemptCount + 1`, `lastError`를 기록한다.
- PostgreSQL은 Flyway `V5__add_outbox_relay_state.sql`로 컬럼을 추가한다.
- H2 빠른 저장소 테스트용 `schema.sql`에도 같은 컬럼을 반영한다.
- 기준 결정은 ADR-0015를 따른다.

### Issue #27 구현 기준

- `PENDING` 또는 retry 가능한 `FAILED` event를 제한 개수만큼 claim한다.
- claim된 event는 `PROCESSING` 상태로 전이한다.
- PostgreSQL은 `FOR UPDATE SKIP LOCKED`로 claim 대상 row를 잠근다.
- 발행 실패 시 `FAILED`, `attemptCount + 1`, `lastError`, `nextRetryAt`을 기록한다.
- 발행 성공 시 `PUBLISHED`, `publishedAt`을 기록하고 retry/error 필드를 초기화한다.
- PostgreSQL은 Flyway `V6__add_outbox_retry_schedule.sql`로 `next_retry_at` 컬럼을 추가한다.
- 기준 결정은 ADR-0016을 따른다.

### Issue #29 구현 기준

- claim된 event는 `claimedAt`, `leaseExpiresAt`을 가진다.
- lease 길이는 60초로 고정한다.
- lease가 만료되지 않은 `PROCESSING` event는 claim 대상이 아니다.
- lease가 만료된 `PROCESSING` event는 다시 claim할 수 있다.
- 발행 성공/실패 시 lease 필드는 초기화한다.
- PostgreSQL은 Flyway `V7__add_outbox_processing_lease.sql`로 lease 컬럼을 추가한다.
- 기준 결정은 ADR-0017을 따른다.

### Issue #31 구현 기준

- outbox event 자동 발행 시도는 최대 3회로 제한한다.
- 1~2회 실패 event는 `FAILED` 상태로 남고 `nextRetryAt` 이후 다시 claim할 수 있다.
- 3회 실패 event는 `MANUAL_REVIEW` 상태로 전이한다.
- `MANUAL_REVIEW` event는 자동 claim 대상이 아니다.
- `MANUAL_REVIEW` event는 `lastError`와 `attemptCount`를 유지한다.
- 기준 결정은 ADR-0018을 따른다.
