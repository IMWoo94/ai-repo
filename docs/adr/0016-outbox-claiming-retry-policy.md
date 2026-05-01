# ADR-0016: Outbox Claiming and Retry Policy

## 상태

Accepted

## 맥락

ADR-0015에서 outbox relay 상태와 실패 메타데이터를 추가했다. 하지만 relay가 pending event를 단순 조회하면 다중 worker 환경에서 같은 event를 동시에 가져갈 수 있다.

또한 실패 event는 `FAILED`로만 남고 언제 다시 시도할지 기준이 없었다. 실제 broker adapter를 붙이기 전이라도, event를 안전하게 가져가는 claiming 경계와 최소 retry schedule이 필요하다.

## 선택지

### 선택지 A: 단순 pending 조회 유지

장점:

- 구현이 가장 단순하다.
- 현재 실제 scheduler와 broker가 없으므로 당장 동작은 충분하다.

단점:

- 여러 relay worker가 같은 event를 중복 처리할 수 있다.
- 실패 event 재시도 기준이 없다.
- MSA 전환 학습에서 중요한 비동기 정합성 경계를 검증하기 어렵다.

### 선택지 B: `PROCESSING` 상태와 고정 backoff 추가

장점:

- `PENDING` 또는 retry 가능한 `FAILED` event만 claim 대상으로 제한할 수 있다.
- claim된 event는 `PROCESSING`으로 전이되어 중복 처리 가능성을 줄인다.
- 고정 backoff로 실패 직후 무한 재시도를 막을 수 있다.
- 실제 scheduler와 broker adapter는 후속 작업으로 분리할 수 있다.

단점:

- retry 정책이 아직 단순하다.
- worker crash로 `PROCESSING`에 오래 머무는 event를 회수하는 lease/timeout 정책은 없다.

### 선택지 C: lease timeout, max attempt, DLQ까지 즉시 구현

장점:

- 운영에 더 가까운 outbox relay 모델이 된다.
- 장애 복구와 폐기 정책까지 한 번에 검증할 수 있다.

단점:

- 현재 학습 단계 대비 범위가 크다.
- broker adapter가 없는 상태에서 DLQ와 lease 정책을 확정하면 추후 변경 가능성이 높다.

## 결정

`PROCESSING` 상태와 고정 backoff 추가를 선택한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| claim 대상 | `PENDING` 또는 `nextRetryAt <= now`인 `FAILED` event |
| claim 상태 | claim 즉시 `PROCESSING`으로 전이 |
| PostgreSQL 동시성 | `FOR UPDATE SKIP LOCKED`를 사용 |
| H2 테스트 | `FOR UPDATE` fallback으로 repository 동작을 검증 |
| 실패 처리 | `FAILED`, `attemptCount + 1`, `lastError`, `nextRetryAt = now + 30초` |
| 발행 성공 | `PUBLISHED`, `publishedAt` 기록, retry/error 필드 초기화 |
| DB 변경 | Flyway `V6__add_outbox_retry_schedule.sql` |
| 범위 제외 | scheduler, broker adapter, lease timeout, DLQ |

## 결과

장점:

- 다중 relay worker를 고려한 첫 claiming 경계가 생긴다.
- 실패 event가 즉시 무한 재시도되지 않는다.
- 후속 broker adapter와 scheduler가 사용할 명확한 service/repository API가 생긴다.

비용:

- `PROCESSING` event가 worker crash 후 고착될 수 있다.
- backoff는 고정 30초로 단순하다.
- max attempt와 DLQ 정책은 아직 없다.

후속 작업:

- `PROCESSING` lease timeout과 recovery 정책을 정의한다.
- max attempt와 DLQ 또는 manual review 상태를 결정한다.
- 실제 scheduler/poller와 broker adapter를 추가한다.
- consumer idempotency key와 event schema versioning을 설계한다.
