# Broker Consumer Idempotency

## 배경

Outbox relay는 최소 1회 발행 모델이다. Broker publish 성공 후 DB 결과 갱신이 실패하거나 lease가 만료되면 같은 outbox event가 다시 발행될 수 있다.

금융 도메인에서는 같은 event가 consumer에서 두 번 처리되면 원장, 알림, 정산, 운영 감사가 중복될 수 있다. 따라서 broker adapter가 consumer dedupe에 필요한 key와 schema contract를 명시해야 한다.

## 목표

- HTTP broker adapter가 idempotency key를 header와 body에 포함한다.
- schema version과 event type을 header로 제공한다.
- consumer는 `idempotencyKey`를 unique key로 저장해 duplicate side effect를 방지한다.
- contract test와 ADR/Wiki 문서로 정책을 고정한다.

## 완료 기준

- [x] `X-Idempotency-Key` header가 있다.
- [x] `X-Event-Schema-Version` header가 있다.
- [x] `X-Event-Type` header가 있다.
- [x] envelope에 `schemaVersion`이 있다.
- [x] envelope에 `idempotencyKey`가 있다.
- [x] contract test가 header/body를 검증한다.
- [x] ADR, progress, Wiki, release notes가 갱신된다.

## 제외

- Kafka/RabbitMQ/SQS broker-specific consumer adapter 구현
- Kafka/RabbitMQ/SQS adapter 구현
- dedupe TTL과 replay 운영 정책
