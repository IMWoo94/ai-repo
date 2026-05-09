# 0054. Consumer Processed Event Dedupe

## 스펙 목표

Broker에서 같은 outbox event가 중복 전달되어도 consumer가 같은 `idempotencyKey`를 한 번만 처리할 수 있는 저장소 기반 방어선을 만든다.

## 완료 결과

- `OperationOutboxConsumerIdempotencyRepository` port를 추가했다.
- `OperationOutboxConsumerProcessedEvent` record를 추가했다.
- `operation_outbox_consumer_processed_events` Flyway migration을 추가했다.
- H2 schema baseline에도 같은 table을 반영했다.
- JDBC repository와 in-memory repository가 processed-event 기록/조회 계약을 구현한다.
- H2 repository test와 PostgreSQL Testcontainers 동시성 test를 추가했다.

## 검증

- `./gradlew test --tests "*JdbcWalletRepositoryTest"`
- `./gradlew postgresScenarioTest --tests "*PostgresContainerWalletRepositoryTest"`

## 남은 일

- 실제 broker consumer adapter
- business side effect와 processed-event 기록의 transaction 통합
- processed-event TTL/pruning 정책

## 관련 문서

- `docs/adr/0044-consumer-processed-event-dedupe.md`
- `docs/adr/0043-broker-consumer-idempotency.md`
