# ADR-0020: Outbox Requeue Audit Trail

## 상태

Accepted

## 맥락

ADR-0019에서 `MANUAL_REVIEW` outbox event를 API로 requeue할 수 있게 했다. 하지만 requeue는 운영자가 실패 event를 다시 자동 처리 흐름에 넣는 행위이므로, 금융/핀테크 운영에서는 그 행위 자체가 감사 대상이다.

따라서 requeue 요청자는 누구인지, 왜 requeue했는지, 언제 수행했는지를 별도 이력으로 남겨야 한다.

## 선택지

### 선택지 A: requeue 이력을 남기지 않는다

장점:

- 구현이 단순하다.
- 기존 requeue API만으로 기능은 동작한다.

단점:

- 운영 조치의 근거를 추적할 수 없다.
- 장애 회고와 감사 대응에 취약하다.
- DB 상태 변경이 왜 발생했는지 나중에 설명하기 어렵다.

### 선택지 B: requeue 감사 이력을 별도 테이블에 남긴다

장점:

- operator, reason, requeuedAt을 구조화해서 조회할 수 있다.
- requeue 행위와 outbox event 상태를 분리해 추적할 수 있다.
- 후속 관리자 인증/인가, 승인 워크플로우, 알림 연동의 기반이 된다.

단점:

- 아직 실제 로그인 사용자와 operator가 연결되지 않는다.
- reason의 품질은 시스템이 검증하지 못한다.
- 승인 워크플로우까지는 포함하지 않는다.

### 선택지 C: 기존 audit event에 문자열로 남긴다

장점:

- 새 테이블이 필요 없다.
- 기존 감사 로그 흐름을 재사용할 수 있다.

단점:

- outbox event별 requeue 이력을 구조적으로 조회하기 어렵다.
- operator/reason 필드를 명확히 강제하기 어렵다.

## 결정

`requeue 감사 이력을 별도 테이블에 남긴다`를 선택한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 저장 테이블 | `operation_outbox_requeue_audits` |
| 필수 필드 | `auditId`, `outboxEventId`, `operationId`, `requeuedAt`, `operator`, `reason` |
| requeue 요청 | `operator`, `reason` 필수 |
| 감사 조회 API | `GET /api/v1/outbox-events/{outboxEventId}/requeue-audits` |
| requeue 처리 | event 상태 변경과 감사 이력 저장을 같은 트랜잭션에서 수행 |
| DB 변경 | Flyway `V8__create_outbox_requeue_audits.sql` |
| 범위 제외 | 인증/인가, 승인 워크플로우, 알림/모니터링 |

## 결과

장점:

- requeue 행위가 감사 가능한 기록으로 남는다.
- 상태 전이와 운영 조치 이력을 분리해서 조회할 수 있다.
- 실제 관리자 기능으로 확장할 때 필요한 최소 데이터가 생긴다.

비용:

- operator는 아직 인증된 사용자와 연결되지 않는다.
- 승인자와 요청자 분리, 2인 승인 같은 정책은 없다.
- requeue 사유의 품질은 운영 절차로 보완해야 한다.

후속 작업:

- 관리자 인증/인가를 추가해 operator를 인증 주체와 연결한다.
- requeue 승인 워크플로우와 알림 정책을 설계한다.
- event schema versioning과 consumer idempotency key를 설계한다.
