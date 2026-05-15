# ADR-0046: HTTP Publish Consume Loop Verification

## 상태

Accepted

## 배경

HTTP publisher contract와 HTTP consumer adapter는 각각 검증됐다. 하지만 두 테스트가 분리되어 있으면 relay가 실제 HTTP publisher로 event를 보내고, 같은 프로세스의 consumer endpoint가 이를 받아 receipt를 남기는지 한 번에 보장하지 못한다.

포트폴리오형 금융 학습 프로젝트에서는 정합성 주장의 근거가 중요하다. 따라서 producer outbox와 consumer inbox/dedupe가 하나의 loop로 연결되는 검증이 필요하다.

## 결정

Spring Boot random port 서버를 띄우고 `HttpOperationOutboxPublisher`를 실제 `/internal/broker/outbox-events` endpoint로 연결하는 통합 테스트를 추가한다.

검증 흐름은 다음과 같다.

1. 지갑 충전으로 outbox event를 생성한다.
2. 별도 `OperationOutboxRelayService`를 구성해 publisher endpoint를 random port consumer endpoint로 지정한다.
3. relay가 `publishReadyEvents`를 실행한다.
4. HTTP consumer가 event를 수신하고 processed-event와 receipt를 기록한다.
5. outbox event는 `PUBLISHED`가 되고 consumer receipt는 1건 남는다.

## 트레이드오프

### 장점

- producer와 consumer를 실제 HTTP로 연결해 분리 테스트보다 강한 회귀 증거를 만든다.
- Kafka/RabbitMQ/SQS 도입 전에도 outbox relay → HTTP broker adapter → HTTP consumer endpoint → dedupe receipt 흐름을 검증할 수 있다.
- 랜덤 포트를 사용하므로 로컬/CI 포트 충돌 가능성이 낮다.

### 비용

- 테스트가 일반 단위 테스트보다 무겁다.
- durable broker의 ack/nack, offset, replay semantics는 여전히 검증하지 않는다.
- 현재는 in-memory profile loop이며 PostgreSQL profile은 consumer endpoint duplicate scenario로 보완한다.

## 검증 기준

- relay publish 결과가 claimed 1, published 1, failed 0이다.
- consumer receipt가 `outbox-001` 기준으로 남는다.
- 같은 relay를 다시 실행해도 추가 claim은 없다.
- operation outbox event는 `PUBLISHED` 상태다.

## 후속 작업

- broker-specific Testcontainers contract에서 publish→consume loop를 재현한다.
- duplicate time bucket metric으로 최근 window 기준 이상 징후를 계산한다.
- local smoke에 HTTP consumer duplicate no-op 확인을 추가한다.
