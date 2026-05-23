# Consumer Duplicate Time Bucket Metric

## 배경

Consumer duplicate health가 누적 지표만 보면 최근 spike를 놓칠 수 있다. 운영자는 최근 N분 동안 duplicate delivery가 급증했는지 확인할 수 있어야 한다.

## 목표

- consumer delivery를 분 단위 bucket으로 집계한다.
- window metric API를 제공한다.
- health summary가 window duplicate rate 기준으로 `OK`, `WARNING`, `CRITICAL`, `NO_DATA`를 판정한다.

## 완료 조건

- [x] `operation_outbox_consumer_delivery_metrics` migration이 존재한다.
- [x] consumer consume 트랜잭션에서 processed/duplicate delivery metric을 기록한다.
- [x] `GET /api/v1/outbox-consumer/window-metrics?minutes=5`가 window metric을 반환한다.
- [x] `GET /api/v1/outbox-consumer/health`가 window metric과 alert 판정을 반환한다.
- [x] service/API/JDBC repository 테스트가 존재한다.

## 검증 명령

```bash
./gradlew test --tests "*OperationOutboxConsumerServiceTest" --tests "*OperationOutboxConsumerMonitoringServiceTest" --tests "*OperationOutboxConsumerMonitoringControllerTest" --tests "*JdbcWalletRepositoryTest"
```
