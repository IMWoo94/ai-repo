# 0060. Consumer Duplicate Time Bucket Metric

## 스펙 목표

Consumer duplicate health를 누적 지표가 아니라 최근 window 기준으로 판단할 수 있게 만든다.

## 완료 결과

- `operation_outbox_consumer_delivery_metrics` 분 단위 bucket 테이블을 추가했다.
- consumer consume 트랜잭션에서 processed/duplicate delivery metric을 함께 기록한다.
- `GET /api/v1/outbox-consumer/window-metrics?minutes=5` API를 추가했다.
- `GET /api/v1/outbox-consumer/health` 응답에 window 시작/종료, window processed/duplicate count, window duplicate rate를 포함했다.
- health 판정은 기본 최근 5분 window 기준으로 수행한다.

## 검증

- `./gradlew test --tests "*OperationOutboxConsumerServiceTest" --tests "*OperationOutboxConsumerMonitoringServiceTest" --tests "*OperationOutboxConsumerMonitoringControllerTest" --tests "*JdbcWalletRepositoryTest"`

## 남은 일

- pruning 실행 이력 저장
- Slack/Webhook push channel
- broker-specific duplicate baseline

## 관련 문서

- `docs/adr/0050-consumer-duplicate-time-bucket-metric.md`
- `docs/releases/unreleased.md`
- `wiki-drafts/QA-Scenarios.md`
