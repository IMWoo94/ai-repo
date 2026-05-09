# ADR-0043: Broker and Consumer Idempotency Contract

## 상태

Accepted

## 배경

Outbox relay는 claim 기반 결과 갱신으로 stale writer가 DB 상태를 덮어쓰지 못하게 됐다. 그러나 외부 broker publish는 네트워크 I/O이므로 다음 상황이 남는다.

- worker가 broker에 event를 보낸 뒤 DB `PUBLISHED` 갱신 전에 실패할 수 있다.
- lease가 만료되면 같은 outbox event가 다시 claim되어 broker로 재발행될 수 있다.
- consumer가 같은 event를 두 번 받으면 원장, 알림, 외부 정산 같은 후속 처리가 중복될 수 있다.

금융/핀테크 흐름에서는 “최소 1회 발행”을 허용하더라도 consumer가 같은 업무 event를 한 번만 처리할 수 있는 계약이 필요하다.

## 결정

HTTP broker adapter의 발행 envelope와 header에 명시적인 idempotency 계약을 추가한다.

| 항목 | 결정 |
| --- | --- |
| schema version | `1` |
| broker idempotency key | `outboxEventId` |
| consumer idempotency key | `idempotencyKey` = `outboxEventId` |
| header | `X-Idempotency-Key` |
| schema header | `X-Event-Schema-Version` |
| type header | `X-Event-Type` |
| body field | `schemaVersion`, `idempotencyKey`, `outboxEventId` |

Consumer 구현 기준은 다음과 같다.

- consumer는 `idempotencyKey`를 processed-event 저장소에 unique key로 저장한다.
- 이미 처리한 `idempotencyKey`를 다시 받으면 성공 처리된 duplicate으로 간주하고 business side effect를 반복하지 않는다.
- broker header와 body의 idempotency key가 다르면 메시지를 처리하지 않고 quarantine 또는 manual review 대상으로 분리한다.
- schema version이 지원 범위를 벗어나면 side effect 없이 실패 처리한다.

## 트레이드오프

### 장점

- relay의 최소 1회 발행 모델을 유지하면서 consumer 중복 처리 위험을 줄인다.
- broker 제품을 Kafka/RabbitMQ/SQS로 바꾸더라도 동일한 key 계약을 유지할 수 있다.
- contract test가 header와 envelope를 고정하므로 adapter 변경 시 호환성 깨짐을 빠르게 찾을 수 있다.

### 비용

- 실제 consumer adapter와 side effect transaction 통합은 아직 구현하지 않는다.
- consumer 저장소 장애, dedupe TTL, replay 정책은 별도 설계가 필요하다.
- `outboxEventId`를 idempotency key로 사용하므로 같은 business operation의 새 event는 별도 처리 단위가 된다.

## 검증 기준

- HTTP broker adapter는 `X-Outbox-Event-Id`, `X-Idempotency-Key`, `X-Event-Schema-Version`, `X-Event-Type`을 함께 전송한다.
- HTTP envelope는 `schemaVersion`, `idempotencyKey`, `outboxEventId`를 포함한다.
- contract test는 header와 body 계약을 검증한다.
- Wiki와 release candidate 문서는 consumer idempotency를 완료된 계약으로 갱신한다.

## 후속 작업

- 실제 consumer adapter를 추가할 때 business side effect와 processed-event 기록을 같은 transaction boundary로 묶는다.
- Kafka/RabbitMQ/SQS adapter별 partition/routing key와 idempotency key 매핑을 검증한다.
- dedupe 보관 기간과 replay 운영 정책을 별도 ADR로 결정한다.
