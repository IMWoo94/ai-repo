# ADR-0044: Consumer Processed Event Dedupe Store

## 상태

Accepted

## 배경

ADR-0043에서 broker envelope에 `idempotencyKey`와 `schemaVersion`을 추가했다. 하지만 계약만으로는 consumer 중복 처리가 막히지 않는다.

Outbox relay는 최소 1회 발행 모델이다. 동일 event가 broker를 통해 두 번 도착할 수 있으므로 consumer는 side effect 실행 전에 이미 처리한 event인지 판단할 수 있어야 한다.

## 결정

consumer processed-event 저장소를 도입한다.

| 항목 | 결정 |
| --- | --- |
| table | `operation_outbox_consumer_processed_events` |
| primary key | `idempotency_key` |
| 저장 필드 | `outbox_event_id`, `event_type`, `processed_at` |
| repository port | `OperationOutboxConsumerIdempotencyRepository` |
| duplicate 처리 | primary key 충돌이면 `false` 반환 |

Consumer 구현 기준은 다음과 같다.

- consumer는 같은 DB 안에서 business side effect와 processed-event 기록을 하나의 트랜잭션으로 묶어야 한다.
- duplicate이면 side effect를 실행하지 않고 성공 처리된 중복 메시지로 간주한다.
- HTTP broker consumer adapter는 ADR-0045에서 실제 endpoint와 receipt side effect까지 연결한다.

## 트레이드오프

### 장점

- 중복 수신 시 같은 `idempotencyKey`가 한 번만 기록된다.
- 실제 consumer 구현 전에 필요한 DB 제약과 repository contract를 고정한다.
- PostgreSQL 동시성 테스트로 unique constraint가 경합 상황에서도 동작하는지 확인한다.

### 비용

- 실제 consumer side effect와 같은 transaction boundary는 아직 구현하지 않는다.
- processed-event 보관 기간, replay, cleanup 정책은 후속 결정이 필요하다.
- 외부 시스템 side effect는 같은 DB transaction으로 묶을 수 없으므로 별도 outbox/inbox 패턴이 필요할 수 있다.

## 검증 기준

- H2 repository test에서 같은 `idempotencyKey`의 두 번째 기록은 `false`를 반환한다.
- PostgreSQL Testcontainers에서 동시 duplicate 기록 중 하나만 성공한다.
- Flyway migration과 H2 schema baseline이 같은 table 구조를 가진다.

## 후속 작업

- Kafka/RabbitMQ/SQS consumer adapter를 추가하고 ack/nack과 dedupe boundary를 검증한다.
- broker replay window별 processed-event retention 권장값을 정의한다.
- broker별 replay와 DLQ 재처리 정책을 설계한다.
