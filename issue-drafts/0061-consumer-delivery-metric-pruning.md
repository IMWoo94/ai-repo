# Consumer Delivery Metric Pruning

## 배경

`operation_outbox_consumer_delivery_metrics`는 최근 duplicate spike 판정에 필요하지만, 보존 기간이 없으면 계속 증가한다.

## 목표

- 기존 consumer pruning run에 delivery metric bucket 삭제를 포함한다.
- 보존 기간을 설정값으로 분리한다.
- API 응답에서 metric bucket 삭제 결과를 확인할 수 있게 한다.

## 완료 조건

- [x] delivery metric retention 설정이 존재한다.
- [x] cutoff 이전 `bucket_started_at` bucket만 삭제한다.
- [x] pruning API 응답에 `deliveryMetricCutoff`, `deletedDeliveryMetricBucketCount`가 포함된다.
- [x] scheduler가 delivery metric retention을 함께 전달한다.
- [x] service/API/JDBC/scheduler 테스트가 존재한다.

## 검증 명령

```bash
./gradlew test --tests "*OperationOutboxConsumerPruningServiceTest" --tests "*OperationOutboxConsumerPruningControllerTest" --tests "*OperationOutboxConsumerPruningSchedulerTest" --tests "*JdbcWalletRepositoryTest"
```
