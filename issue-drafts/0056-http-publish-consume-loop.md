# HTTP Publish Consume Loop

## 배경

HTTP publisher와 HTTP consumer는 각각 테스트됐지만, relay가 실제 HTTP publisher를 통해 consumer endpoint로 event를 보내는 loop 검증은 없었다.

## 목표

- 실제 Spring Boot random port 서버를 사용한다.
- `HttpOperationOutboxPublisher`가 `/internal/broker/outbox-events`로 event를 POST한다.
- consumer receipt가 남고 outbox event가 `PUBLISHED`가 되는지 검증한다.
- 같은 relay를 재실행해 추가 claim이 없는지 확인한다.

## 완료 기준

- [x] HTTP publish→consume loop test가 있다.
- [x] relay publish result가 `claimed=1`, `published=1`, `failed=0`이다.
- [x] consumer receipt가 남는다.
- [x] outbox event가 `PUBLISHED` 상태가 된다.
- [x] random port를 사용해 포트 충돌을 피한다.

## 제외

- Kafka/RabbitMQ/SQS broker loop
- durable broker ack/nack 검증
- local smoke script 확장
