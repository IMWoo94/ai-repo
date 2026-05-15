# HTTP Outbox Consumer Adapter

## 배경

Broker/consumer idempotency 계약과 processed-event 저장소는 준비됐지만 실제 메시지를 받는 adapter가 없었다. 이 상태에서는 “중복 수신에도 side effect가 한 번만 발생한다”는 end-to-end 증거가 부족하다.

## 목표

- HTTP broker consumer endpoint를 추가한다.
- header/body schema contract를 검증한다.
- processed-event 기록과 consumer receipt side effect를 같은 service transaction으로 처리한다.
- duplicate message는 성공 no-op으로 처리한다.
- PostgreSQL scenario에서 실제 endpoint를 두 번 호출해 receipt가 한 번만 남는지 검증한다.

## 완료 기준

- [x] consumer envelope/result 모델이 있다.
- [x] consumer service가 schema version과 idempotency key를 검증한다.
- [x] HTTP consumer controller가 있다.
- [x] consumer receipt table migration이 있다.
- [x] duplicate 수신 시 receipt가 중복 저장되지 않는다.
- [x] PostgreSQL scenario에서 endpoint 기반 duplicate 처리를 검증한다.

## 제외

- Kafka/RabbitMQ/SQS consumer group 처리
- external broker ack/nack 정책
- consumer metric/admin UI
