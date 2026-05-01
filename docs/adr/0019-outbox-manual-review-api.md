# ADR-0019: Outbox Manual Review API

## 상태

Accepted

## 맥락

ADR-0018에서 outbox event가 최대 3회 실패하면 `MANUAL_REVIEW` 상태로 격리하기로 했다. 이 상태는 자동 claim 대상에서 제외되므로, 운영자가 격리된 event를 확인하고 원인 조치 후 다시 처리 흐름으로 넣을 수 있는 최소 API가 필요하다.

## 선택지

### 선택지 A: DB 직접 조회와 수동 수정

장점:

- 구현이 필요 없다.
- 초기 실험에서는 빠르게 확인할 수 있다.

단점:

- 운영 절차가 코드와 테스트로 고정되지 않는다.
- 실수로 상태나 retry 메타데이터를 잘못 수정할 수 있다.
- 포트폴리오형 운영 하네스의 검증 흐름에 맞지 않는다.

### 선택지 B: manual review 조회와 requeue API 제공

장점:

- 격리된 event를 표준 API로 조회할 수 있다.
- requeue 정책을 코드와 테스트로 고정할 수 있다.
- 실제 관리자 UI, 인증/인가, 감사 이력 추가 전 최소 운영 흐름을 검증할 수 있다.

단점:

- 아직 인증/인가가 없어서 실제 운영 API로는 부족하다.
- requeue 승인자와 사유를 별도 감사 테이블에 남기지 않는다.
- 원인 조치 없이 requeue하면 같은 실패가 반복될 수 있다.

### 선택지 C: 관리자 승인 이력과 재처리 요청 모델까지 구현

장점:

- 운영 감사와 승인 흐름에 더 가깝다.
- 누가, 왜, 언제 requeue 했는지 추적할 수 있다.

단점:

- 현재 단계 대비 범위가 크다.
- 인증/인가와 관리자 계정 모델을 먼저 결정해야 한다.

## 결정

`manual review 조회와 requeue API 제공`을 선택한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 조회 API | `GET /api/v1/outbox-events/manual-review?limit=50` |
| 재처리 API | `POST /api/v1/outbox-events/{outboxEventId}/requeue` |
| 조회 대상 | `MANUAL_REVIEW` 상태 event |
| requeue 가능 대상 | `MANUAL_REVIEW` 상태 event만 허용 |
| requeue 결과 | `PENDING`, `attemptCount = 0`, retry/lease/publish/error 필드 초기화 |
| 응답 | requeue 성공 시 `204 No Content` |
| 범위 제외 | 인증/인가, 승인 이력 테이블, 알림/모니터링 |

## 결과

장점:

- 자동 retry에서 제외된 event를 운영 흐름으로 다시 넣을 수 있다.
- requeue 정책이 DB 직접 수정이 아니라 코드로 고정된다.
- 후속 관리자 UI와 감사 이력의 API 기반이 생긴다.

비용:

- requeue 사유와 승인자를 저장하지 않는다.
- 원인 조치 여부를 시스템이 검증하지 않는다.
- 실제 운영 사용 전 인증/인가가 반드시 필요하다.

후속 작업:

- manual review requeue 감사 로그를 별도 테이블로 남긴다.
- 관리자 인증/인가 정책을 추가한다.
- event schema versioning과 consumer idempotency key를 설계한다.
