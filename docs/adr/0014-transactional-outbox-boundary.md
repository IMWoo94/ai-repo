# ADR-0014: Transactional Outbox Boundary

## 상태

Accepted

## 맥락

ADR-0013에서 operation step log를 추가해 충전/송금 처리 과정을 관측할 수 있게 했다. 하지만 step log는 내부 처리 과정 기록이며, 외부 서비스로 전달할 integration event가 아니다.

추후 MSA로 전환하면 송금 완료 후 알림, 정산, 리포팅, 이상거래 탐지 같은 다른 서비스가 돈 이동 결과에 반응해야 한다. 이때 돈 이동 트랜잭션은 성공했지만 이벤트 발행은 실패하는 이중 쓰기 문제가 생길 수 있다.

현재 단계에서 메시지 브로커와 relay를 모두 도입하면 학습 범위가 커진다. 따라서 먼저 돈 이동 결과와 outbox event 적재를 같은 DB 트랜잭션으로 묶는 경계를 만든다.

## 선택지

### 선택지 A: Step Log만 유지

장점:

- 구현이 단순하다.
- 추가 테이블이 필요 없다.

단점:

- 외부 서비스에 전달할 이벤트 경계가 없다.
- step log를 integration event로 오용할 수 있다.
- MSA 전환 시 이중 쓰기 문제를 별도로 해결해야 한다.

### 선택지 B: Transactional Outbox 저장만 추가

장점:

- 돈 이동 결과와 이벤트 적재를 같은 DB 트랜잭션 안에서 보장한다.
- 메시지 브로커 없이도 MSA 전환의 이벤트 경계를 코드와 DB로 남긴다.
- relay, retry, consumer idempotency를 후속 작업으로 분리할 수 있다.

단점:

- 아직 실제 이벤트 발행은 하지 않는다.
- event payload versioning과 재시도 정책이 후속으로 필요하다.

### 선택지 C: Kafka/Saga/Relay 즉시 도입

장점:

- 분산 시스템 전환을 실제 환경에 가깝게 검증한다.
- 발행, 재시도, consumer 처리까지 end-to-end로 볼 수 있다.

단점:

- 현재 모놀리스 학습 단계에 비해 인프라 복잡도가 크다.
- 메시지 브로커 운영, relay, idempotent consumer 설계가 필요하다.
- 핵심 도메인 정합성 학습보다 도구 도입 비용이 앞설 수 있다.

## 결정

`Transactional Outbox 저장만 추가`를 선택한다.

초기 구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 기록 대상 | 충전/송금 성공 operation |
| 저장 시점 | 돈 이동 결과와 같은 저장소 트랜잭션 |
| 초기 상태 | `PENDING` |
| 이벤트 타입 | `CHARGE_COMPLETED`, `TRANSFER_COMPLETED` |
| aggregate | `WALLET_OPERATION`, operation id |
| DB 변경 | Flyway `V4__create_operation_outbox_events.sql` |
| 조회 API | `GET /api/v1/operations/{operationId}/outbox-events` |
| 발행 정책 | relay/publisher는 후속 작업 |

## 결과

장점:

- 성공한 돈 이동 결과와 외부 반응 후보가 같은 트랜잭션으로 남는다.
- step log와 outbox event의 책임이 분리된다.
- 추후 MSA 전환 시 relay/publisher 구현을 붙일 수 있는 테이블 경계가 생긴다.

비용:

- outbox 테이블과 payload 관리가 추가된다.
- 아직 이벤트 발행, 재시도, 폐기 상태 전이는 없다.
- payload schema versioning이 후속 결정으로 남는다.

후속 작업:

- Outbox relay/publisher를 구현할지 결정한다.
- event payload schema version을 추가할지 결정한다.
- consumer idempotency key와 재시도/폐기 상태 전이를 정의한다.
