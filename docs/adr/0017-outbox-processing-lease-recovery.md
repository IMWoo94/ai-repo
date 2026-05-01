# ADR-0017: Outbox Processing Lease Recovery

## 상태

Accepted

## 맥락

ADR-0016에서 outbox event를 claim할 때 `PROCESSING` 상태로 전이하고, PostgreSQL에서는 `FOR UPDATE SKIP LOCKED`를 사용하기로 했다.

남은 문제는 relay worker가 event를 `PROCESSING`으로 claim한 뒤 crash 되는 경우다. 이 경우 event가 영구적으로 처리 중 상태에 머물 수 있으므로, 처리 lease와 만료 후 recovery 정책이 필요하다.

## 선택지

### 선택지 A: `PROCESSING` 상태를 수동 복구한다

장점:

- 구현이 단순하다.
- 자동 회수로 인한 중복 발행 가능성을 피할 수 있다.

단점:

- 운영자가 직접 DB 상태를 확인해야 한다.
- 장애 복구가 느리고 재현 가능한 정책으로 남지 않는다.
- 학습 목표인 운영 하네스 관점에서 자동 복구 경계가 부족하다.

### 선택지 B: claim 시 lease를 기록하고 만료 시 재claim한다

장점:

- worker crash 후에도 시간이 지나면 event가 다시 처리 대상이 된다.
- `claimedAt`, `leaseExpiresAt`으로 처리 중 상태의 관측성이 좋아진다.
- scheduler와 broker adapter 없이도 recovery 경계를 테스트할 수 있다.

단점:

- lease 만료 후 재claim되면 이전 worker가 늦게 발행하는 경우 중복 발행 가능성이 있다.
- consumer idempotency가 아직 없으므로 실제 broker 연결 전 추가 방어가 필요하다.

### 선택지 C: heartbeat 기반 lease 연장까지 구현한다

장점:

- 긴 발행 작업도 lease를 유지할 수 있다.
- worker 상태를 더 정확히 반영한다.

단점:

- 현재 broker adapter와 scheduler가 없어서 범위가 크다.
- heartbeat 실패, clock skew, worker identity까지 함께 설계해야 한다.

## 결정

`claim 시 lease를 기록하고 만료 시 재claim`을 선택한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| claim 메타데이터 | `claimedAt`, `leaseExpiresAt` |
| lease 길이 | 고정 60초 |
| claim 대상 | `PENDING`, retry 가능한 `FAILED`, lease가 만료된 `PROCESSING` |
| claim 결과 | `PROCESSING`, `claimedAt = now`, `leaseExpiresAt = now + 60초` |
| 발행 성공 | `PUBLISHED`로 전이하고 lease/retry/error 필드를 초기화 |
| 발행 실패 | `FAILED`로 전이하고 lease 필드를 초기화, retry schedule 기록 |
| DB 변경 | Flyway `V7__add_outbox_processing_lease.sql` |
| 범위 제외 | heartbeat, worker identity, max attempt, DLQ, 실제 broker 발행 |

## 결과

장점:

- `PROCESSING` 고착 event를 자동 회수할 수 있는 최소 정책이 생긴다.
- 처리 중 event의 시작 시각과 만료 시각을 DB에서 확인할 수 있다.
- 실제 scheduler/poller 도입 전 recovery 규칙을 테스트로 고정한다.

비용:

- lease 만료 후 중복 발행 가능성은 여전히 남는다.
- worker identity가 없어 어떤 worker가 claim했는지는 알 수 없다.
- max attempt와 DLQ 정책은 아직 없다.

후속 작업:

- consumer idempotency key와 event schema versioning을 먼저 설계한다.
- broker adapter를 붙일 때 중복 발행과 중복 소비를 방어한다.
- 필요 시 worker identity와 heartbeat 기반 lease 연장을 도입한다.
