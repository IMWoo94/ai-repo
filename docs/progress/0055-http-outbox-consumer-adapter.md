# 0055. HTTP Outbox Consumer Adapter

## 스펙 목표

Broker event를 실제 HTTP endpoint로 받아 processed-event 기록과 consumer side effect를 하나의 service transaction으로 처리한다.

## 완료 결과

- `OperationOutboxConsumerEnvelope`와 `OperationOutboxConsumerResult`를 추가했다.
- `OperationOutboxConsumerService`를 추가해 schema 검증, dedupe 기록, receipt 저장을 처리한다.
- `OperationOutboxConsumerController`를 추가해 `POST /internal/broker/outbox-events`를 제공한다.
- `operation_outbox_consumer_receipts` Flyway migration과 H2 schema baseline을 추가했다.
- JDBC/in-memory repository가 consumer receipt 저장/조회 계약을 구현한다.
- API test와 PostgreSQL scenario test로 duplicate side effect 1회 처리를 검증했다.

## 검증

- `./gradlew test --tests "*OperationOutboxConsumerServiceTest" --tests "*OperationOutboxConsumerControllerTest" --tests "*JdbcWalletRepositoryTest"`
- `./gradlew postgresScenarioTest --tests "*PostgresWalletScenarioFlowTest"`

## 남은 일

- Kafka/RabbitMQ/SQS consumer adapter
- consumer 처리 metric/admin API
- broker replay window별 consumer retention 권장값

## 관련 문서

- `docs/adr/0045-http-outbox-consumer-adapter.md`
- `docs/adr/0044-consumer-processed-event-dedupe.md`
- `docs/adr/0043-broker-consumer-idempotency.md`
