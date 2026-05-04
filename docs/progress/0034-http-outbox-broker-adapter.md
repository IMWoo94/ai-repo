# 0034. HTTP Outbox Broker Adapter

## 스펙 목표

- 실제 외부 I/O를 수행하는 outbox publisher adapter를 추가한다.
- 기본 실행은 기존 in-memory publisher를 유지한다.
- HTTP 발행 envelope contract를 테스트로 고정한다.

## 완료 결과

- `HttpOperationOutboxPublisher`를 추가했다.
- `InMemoryOperationOutboxPublisher`를 기본 `memory` publisher로 유지했다.
- `ai-repo.outbox.publisher.type` 설정으로 `memory`, `http` adapter를 선택할 수 있게 했다.
- HTTP adapter가 `POST`와 `application/json`, `X-Outbox-Event-Id` header로 event envelope를 발행한다.
- HTTP 2xx는 성공으로 처리하고, non-2xx/IO/interrupt는 실패로 변환한다.
- JDK HTTP server 기반 contract test로 method, path, header, JSON envelope를 검증했다.
- HTTP broker 실패가 relay service에서 outbox `FAILED` 상태로 이어지는지 검증했다.

## 검증

- `./gradlew test --tests '*HttpOperationOutboxPublisherContractTest' --tests '*OperationOutboxPublisherConfigurationTest' --tests '*OperationOutboxRelayServiceTest'`
- `./gradlew test scenarioTest check`
- `git diff --check`

## 남은 일

- Kafka/RabbitMQ/SQS 중 product-specific broker adapter를 선택한다.
- consumer idempotency key 정책을 설계한다.
- broker 인증/서명과 DLQ 정책을 결정한다.

## 관련 문서

- `docs/adr/0035-http-outbox-broker-adapter.md`
- `docs/development/local-setup.md`
- `issue-drafts/0034-http-outbox-broker-adapter.md`
