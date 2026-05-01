# ADR-0015: Outbox Relay State

## 상태

Accepted

## 맥락

ADR-0014에서 Transactional Outbox 경계를 추가했다. 충전/송금 성공 결과는 돈 이동 저장소 트랜잭션 안에서 `PENDING` outbox event로 적재된다.

남은 문제는 relay가 사용할 상태 전이 모델이다. 실제 브로커를 붙이지 않더라도, 어떤 event가 발행 대기 중인지, 발행이 성공했는지, 실패했다면 몇 번 실패했고 마지막 오류가 무엇인지 기록할 수 있어야 한다.

## 선택지

### 선택지 A: `PENDING` 적재만 유지

장점:

- 구현이 단순하다.
- 현재 실제 relay가 없으므로 당장 필요한 코드가 적다.

단점:

- relay가 처리한 event와 실패한 event를 구분할 수 없다.
- 운영 장애 분석에 필요한 attempt/error 정보가 없다.
- 후속 relay 구현 시 상태 모델을 다시 설계해야 한다.

### 선택지 B: 상태 전이와 attempt 메타데이터 추가

장점:

- relay/publisher 구현 전에도 DB 상태 모델을 검증할 수 있다.
- 발행 성공과 실패를 명확히 기록한다.
- 실제 브로커 어댑터, retry, claiming 정책을 후속 작업으로 분리할 수 있다.

단점:

- 아직 background scheduler나 broker publish는 없다.
- 실패 event를 언제 재시도할지, 언제 폐기할지 정책이 남는다.

### 선택지 C: `SKIP LOCKED` claiming과 relay를 즉시 구현

장점:

- 다중 relay 병렬 처리에 가까운 구조를 바로 검증할 수 있다.
- 실제 발행 파이프라인에 필요한 경합 제어를 조기에 볼 수 있다.

단점:

- 현재 단계 대비 범위가 크다.
- claiming timeout, duplicate publish, consumer idempotency까지 함께 설계해야 한다.

## 결정

`상태 전이와 attempt 메타데이터 추가`를 선택한다.

초기 구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| pending 조회 | `PENDING` event를 발생 순서로 제한 개수 조회 |
| 발행 성공 | `PUBLISHED`로 전이하고 `publishedAt` 기록 |
| 발행 실패 | `FAILED`로 전이하고 `attemptCount` 증가, `lastError` 기록 |
| 신규 event | `attemptCount = 0`, `publishedAt = null`, `lastError = null` |
| DB 변경 | Flyway `V5__add_outbox_relay_state.sql` |
| 실제 발행 | 후속 broker adapter 작업으로 분리 |

## 결과

장점:

- outbox relay가 사용할 최소 상태 전이 경계가 생긴다.
- 돈 이동 결과와 event 발행 상태의 책임을 분리한다.
- 운영 관측에 필요한 실패 횟수와 마지막 오류를 저장한다.

비용:

- retry backoff와 재처리 스케줄은 아직 없다.
- `FAILED` event를 재시도할지 폐기할지 정책이 후속으로 남는다.
- 다중 relay 병렬 처리를 위한 claiming은 아직 없다.

후속 작업:

- `SKIP LOCKED` 기반 claiming 정책을 결정한다.
- retry backoff와 재처리 스케줄을 정의한다.
- broker adapter와 consumer idempotency key 정책을 설계한다.
