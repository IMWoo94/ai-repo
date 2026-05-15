# ADR-0051: Consumer Delivery Metric Pruning

## 상태

Accepted

## 배경

ADR-0050에서 consumer delivery를 분 단위 bucket으로 집계했다. 이 metric은 최근 duplicate spike 판정에 필요하지만, 보존 기간이 없으면 운영 기간에 비례해 계속 증가한다.

Processed-event와 receipt는 이미 consumer pruning run에서 정리한다. Delivery metric도 같은 운영 조치에 포함해야 pruning 책임이 분산되지 않는다.

## 결정

`POST /api/v1/outbox-consumer/pruning-runs`가 processed-event, receipt, delivery metric bucket을 함께 정리한다.

| 항목 | 결정 |
| --- | --- |
| 대상 테이블 | `operation_outbox_consumer_delivery_metrics` |
| 기준 시각 | `bucket_started_at` |
| 기본 보존 기간 | `ai-repo.outbox-consumer-pruning.delivery-metric-retention-days=30` |
| 실행 API | 기존 `POST /api/v1/outbox-consumer/pruning-runs` |
| 권한 | 변경성 운영 조치이므로 admin token 필요 |
| 응답 추가 | `deliveryMetricCutoff`, `deletedDeliveryMetricBucketCount` |
| scheduler | 기존 consumer pruning scheduler가 같은 정책으로 실행 |

## 트레이드오프

### 장점

- window metric 저장소가 무제한 증가하지 않는다.
- 기존 consumer pruning API/scheduler에 합쳐 운영 절차를 단순하게 유지한다.
- metric bucket은 count만 저장하므로 processed-event보다 짧은 보존 기간을 선택해도 business side effect dedupe에는 영향이 없다.

### 비용

- pruning 결과 응답 필드가 늘어나 기존 운영 클라이언트가 새 필드를 무시할 수 있어야 한다.
- metric 보존 기간을 health window보다 짧게 설정하면 health 판단 근거가 사라질 수 있다.
- pruning run 이력 저장은 아직 없다.

## 검증 기준

- cutoff 이전 `bucket_started_at` bucket만 삭제한다.
- cutoff와 같은 시각의 bucket은 유지한다.
- API 응답에 delivery metric cutoff와 삭제 count가 포함된다.
- scheduler는 processed-event, receipt, delivery metric retention을 모두 전달한다.

## 후속 작업

- pruning run 실행 이력을 저장하고 운영자 화면에 노출한다.
- broker-specific adapter별 metric retention 권장값을 문서화한다.
- Slack/Webhook push channel을 연결한다.
