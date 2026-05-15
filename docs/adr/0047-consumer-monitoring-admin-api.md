# ADR-0047: Consumer Monitoring Admin API

## 상태

Accepted

## 배경

HTTP publish→consume loop는 relay가 consumer endpoint까지 event를 전달하고 receipt side effect를 한 번만 남기는 것을 검증한다. 하지만 운영자는 실행 중인 시스템에서 consumer가 몇 건을 처리했고, duplicate event가 no-op으로 얼마나 발생했으며, 최근 receipt가 무엇인지 확인할 수 있어야 한다.

금융/핀테크형 시스템에서는 “중복 소비가 안전하다”는 코드 테스트뿐 아니라 운영자가 확인할 수 있는 지표와 감사 가능한 조회 경로가 필요하다.

## 결정

consumer processed-event 저장소에 `duplicate_count`를 추가하고, 운영 조회 API를 제공한다.

| 항목 | 결정 |
| --- | --- |
| duplicate 기록 | 기존 `idempotencyKey` row의 `duplicate_count`를 증가시킨다 |
| metric API | `GET /api/v1/outbox-consumer/metrics` |
| receipt API | `GET /api/v1/outbox-consumer/receipts?limit=50` |
| 권한 | 조회성 운영 API이므로 `ROLE_OPERATOR` 이상 |
| 감사 | 기존 admin API access audit filter 대상에 포함 |

## 트레이드오프

### 장점

- duplicate event가 단순히 무시되는지뿐 아니라 몇 번 발생했는지 운영 지표로 볼 수 있다.
- receipt 조회로 consumer side effect가 실제 한 번만 남았는지 확인할 수 있다.
- broker-specific adapter 도입 전에도 HTTP loop의 운영 관측 경계를 고정한다.

### 비용

- processed-event row에 duplicate count update가 추가되어 중복 이벤트마다 작은 write 비용이 생긴다.
- duplicate payload의 상세 이력은 저장하지 않는다. 현재 단계에서는 count 중심 관측으로 제한한다.
- broker replay window별 retention 권장값은 아직 별도 후속 과제다.

## 검증 기준

- 같은 `idempotencyKey`를 두 번 소비하면 processed count는 1, duplicate count는 1, receipt count는 1이다.
- monitoring API는 operator token과 operator id 없이는 401을 반환한다.
- recent receipt API는 최신 receipt를 limit 기준으로 반환한다.
- JDBC 저장소와 Spring MVC controller 테스트가 모두 통과한다.

## 후속 작업

- broker replay window별 retention 권장값을 문서화한다.
- delivery metric bucket pruning 정책을 추가한다.
- broker-specific consumer adapter에도 같은 metric 계약을 적용한다.
