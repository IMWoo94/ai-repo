# 0061. Consumer Delivery Metric Pruning

## 스펙 목표

Consumer duplicate window metric 저장소가 무제한 증가하지 않도록 기존 consumer pruning 흐름에 delivery metric bucket 정리를 포함한다.

## 완료 결과

- `OperationOutboxConsumerPruningPolicy`에 delivery metric retention을 추가했다.
- `OperationOutboxConsumerPruningService`가 processed-event, receipt, delivery metric bucket을 함께 삭제한다.
- `POST /api/v1/outbox-consumer/pruning-runs` 응답에 `deliveryMetricCutoff`, `deletedDeliveryMetricBucketCount`를 추가했다.
- 기존 consumer pruning scheduler가 delivery metric retention도 함께 전달한다.
- InMemory/JDBC 저장소에 `bucket_started_at` 기준 metric bucket 삭제를 구현했다.

## 검증

- `./gradlew test --tests "*OperationOutboxConsumerPruningServiceTest" --tests "*OperationOutboxConsumerPruningControllerTest" --tests "*OperationOutboxConsumerPruningSchedulerTest" --tests "*JdbcWalletRepositoryTest"`

## 남은 일

- pruning 실행 이력 저장
- broker-specific retention 권장값
- external alert channel

## 관련 문서

- `docs/adr/0051-consumer-delivery-metric-pruning.md`
- `docs/releases/unreleased.md`
- `wiki-drafts/QA-Scenarios.md`
