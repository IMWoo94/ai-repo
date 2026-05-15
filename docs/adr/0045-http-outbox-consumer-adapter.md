# ADR-0045: HTTP Outbox Consumer Adapter

## 상태

Accepted

## 배경

ADR-0043은 broker envelope의 idempotency key 계약을 고정했고, ADR-0044는 consumer processed-event 저장소를 추가했다. 하지만 실제 inbound adapter가 없으면 “메시지를 받아 한 번만 처리한다”는 end-to-end 증거가 부족하다.

## 결정

HTTP broker consumer adapter를 먼저 도입한다.

| 항목 | 결정 |
| --- | --- |
| inbound endpoint | `POST /internal/broker/outbox-events` |
| application service | `OperationOutboxConsumerService` |
| schema version | `1` |
| duplicate 기준 | `idempotencyKey` |
| side effect 예시 | `operation_outbox_consumer_receipts` receipt 저장 |
| transaction boundary | processed-event 기록과 receipt 저장을 같은 service transaction으로 묶음 |

Consumer 처리 흐름은 다음과 같다.

1. HTTP header와 body의 `outboxEventId`, `idempotencyKey`, `eventType`, `schemaVersion`이 일치하는지 검증한다.
2. `operation_outbox_consumer_processed_events`에 `idempotencyKey`를 기록한다.
3. 첫 처리이면 `operation_outbox_consumer_receipts`에 consumer side effect receipt를 저장한다.
4. duplicate이면 receipt를 다시 저장하지 않고 성공 no-op으로 반환한다.

## 트레이드오프

### 장점

- 실제 HTTP inbound adapter로 producer envelope contract를 소비할 수 있다.
- duplicate message가 들어와도 side effect가 한 번만 발생하는 흐름을 테스트로 고정한다.
- PostgreSQL profile에서 controller, service transaction, JDBC repository, Flyway schema를 함께 검증한다.

### 비용

- Kafka/RabbitMQ/SQS consumer group, offset, ack/nack 모델은 아직 없다.
- receipt는 학습용 side effect이며 실제 정산/알림/외부 호출 side effect는 별도 adapter가 필요하다.
- 외부 시스템 side effect는 DB transaction과 완전히 묶을 수 없으므로 후속으로 outbox/inbox 보상 정책이 필요하다.

## 검증 기준

- consumer service는 같은 envelope를 두 번 받아도 첫 번째만 `processed=true`를 반환한다.
- HTTP consumer endpoint는 header/body 불일치를 side effect 전에 거부한다.
- PostgreSQL scenario는 HTTP endpoint를 두 번 호출해 receipt가 한 번만 남는지 검증한다.

## 후속 작업

- broker별 consumer adapter와 ack/nack 정책을 설계한다.
- consumer receipt를 운영 조회 API와 metric으로 노출한다.
- processed-event와 receipt pruning 정책을 추가한다.
