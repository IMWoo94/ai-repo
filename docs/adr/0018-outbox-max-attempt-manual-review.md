# ADR-0018: Outbox Max Attempt and Manual Review

## 상태

Accepted

## 맥락

ADR-0017에서 `PROCESSING` event가 worker crash로 고착되지 않도록 lease recovery를 추가했다. 이제 outbox relay는 실패 event를 retry schedule에 따라 다시 claim할 수 있다.

남은 문제는 계속 실패하는 event가 무한히 재시도될 수 있다는 점이다. 금융/핀테크 운영에서는 반복 실패 event를 자동 재시도에서 분리해 운영자가 확인할 수 있는 최종 격리 상태가 필요하다.

## 선택지

### 선택지 A: 무한 재시도 유지

장점:

- 구현이 단순하다.
- 일시 장애가 길어도 언젠가 자동 복구될 수 있다.

단점:

- 영구 실패 event가 계속 리소스를 소비한다.
- 운영자가 문제 event를 구분하기 어렵다.
- poison event가 relay 처리량을 떨어뜨릴 수 있다.

### 선택지 B: max attempt 후 `MANUAL_REVIEW`로 격리

장점:

- 반복 실패 event를 자동 retry 흐름에서 분리할 수 있다.
- 운영자가 확인해야 하는 event를 명확히 구분할 수 있다.
- 실제 DLQ broker 없이도 1차 격리 정책을 테스트할 수 있다.

단점:

- 운영자 재처리 API는 아직 없다.
- 실제 DLQ topic/queue와 알림은 후속 작업이다.
- max attempt 값이 고정이면 이벤트 유형별 차등 정책은 어렵다.

### 선택지 C: 즉시 DLQ broker topic/queue 구현

장점:

- 실제 메시징 운영에 더 가깝다.
- 장애 event를 별도 consumer와 알림으로 처리할 수 있다.

단점:

- 아직 broker adapter가 없으므로 범위가 크다.
- DLQ payload, replay, ownership 정책까지 함께 설계해야 한다.

## 결정

`max attempt 후 MANUAL_REVIEW로 격리`를 선택한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 최대 시도 횟수 | 3회 |
| 1~2회 실패 | `FAILED`, `attemptCount + 1`, `nextRetryAt = now + 30초` |
| 3회 실패 | `MANUAL_REVIEW`, `attemptCount = 3`, `nextRetryAt = null` |
| claim 대상 | `PENDING`, retry 가능한 `FAILED`, lease 만료된 `PROCESSING` |
| claim 제외 | `PUBLISHED`, `MANUAL_REVIEW` |
| 운영자 처리 | 후속 수동 재처리 API에서 결정 |
| DB 변경 | status 값 추가만 필요하므로 migration 없음 |

## 결과

장점:

- poison event의 무한 재시도를 차단한다.
- 운영자가 확인해야 할 outbox event를 상태로 분리한다.
- broker DLQ 없이도 manual review 경계를 먼저 검증할 수 있다.

비용:

- `MANUAL_REVIEW` event를 재처리하는 API는 아직 없다.
- max attempt가 고정값이다.
- 알림과 모니터링 연동은 아직 없다.

후속 작업:

- manual review 조회/재처리 API를 설계한다.
- 실제 broker adapter 도입 시 DLQ topic/queue 정책을 결정한다.
- event schema versioning과 consumer idempotency key를 설계한다.
