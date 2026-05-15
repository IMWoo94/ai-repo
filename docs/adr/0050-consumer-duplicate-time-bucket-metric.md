# ADR-0050: Consumer Duplicate Time Bucket Metric

## 상태

Accepted

## 배경

ADR-0049는 duplicate delivery health를 누적 count 기반으로 판정했다. 누적 지표는 전체 안정성을 보여주지만, 최근 몇 분 사이에 duplicate delivery가 급증한 상황을 빠르게 식별하기 어렵다.

금융/핀테크 운영에서는 중복 메시지를 안전하게 no-op 처리하는 것만으로 충분하지 않다. Broker replay, relay 재시도, consumer 장애 복구가 짧은 시간에 몰리면 운영자가 최근 window 기준으로 이상 징후를 볼 수 있어야 한다.

## 결정

Consumer delivery를 분 단위 bucket으로 집계하고, health summary는 기본 최근 5분 window의 duplicate rate로 판정한다.

| 항목 | 결정 |
| --- | --- |
| 저장소 | `operation_outbox_consumer_delivery_metrics` |
| bucket 기준 | `occurredAt`을 분 단위로 절삭한 `bucket_started_at` |
| 기록 시점 | consumer가 idempotency 판정 후 같은 트랜잭션에서 기록 |
| processed delivery | 신규 event 처리 성공 1건 |
| duplicate delivery | 이미 처리된 `idempotencyKey` 재수신 1건 |
| window API | `GET /api/v1/outbox-consumer/window-metrics?minutes=5` |
| health API | `GET /api/v1/outbox-consumer/health`가 window metric을 포함 |
| health window 기본값 | `ai-repo.outbox-consumer.health.window-minutes=5` |
| window 제한 | 1분 이상 1440분 이하 |

## 트레이드오프

### 장점

- 누적 duplicate rate와 최근 window duplicate rate를 분리해 운영 판단을 더 빠르게 한다.
- payload 전문을 저장하지 않고 count만 저장하므로 개인정보/민감정보 노출면을 늘리지 않는다.
- 분 단위 bucket은 개별 attempt history보다 저장량이 작고 pruning 정책을 붙이기 쉽다.

### 비용

- consumer delivery마다 metric upsert write가 1회 추가된다.
- 분 단위 bucket은 초 단위 spike를 세밀하게 표현하지 못한다.
- bucket retention과 pruning 실행 이력은 별도 후속 과제로 남긴다.

## 검증 기준

- 같은 분에 processed와 duplicate delivery가 누적된다.
- window 밖 bucket은 window metric과 health 판정에서 제외된다.
- health API는 누적 지표와 window 지표를 함께 반환한다.
- window metric API는 operator token과 operator id로 조회할 수 있다.

## 후속 작업

- delivery metric bucket retention/pruning 정책을 추가한다.
- external alert channel을 연결한다.
- broker-specific adapter별 duplicate baseline을 문서화한다.
