# HTTP Outbox Broker Adapter and Contract Test

## 배경

Outbox relay는 `OperationOutboxPublisher` port와 in-memory adapter로 claim → publish → published/failed 흐름을 검증한다. 하지만 fake adapter는 실제 외부 I/O, HTTP status, timeout, serialization contract를 검증하지 못한다.

Kafka/RabbitMQ/SQS를 바로 붙이면 로컬/CI 인프라와 의존성 범위가 커진다. 이번 단계에서는 실제 네트워크 I/O를 수행하는 HTTP broker adapter를 먼저 도입하고, JDK HTTP server 기반 contract test로 발행 계약을 고정한다.

## 목표

- `OperationOutboxPublisher`의 실제 외부 I/O adapter를 추가한다.
- 기본 실행은 기존 in-memory publisher를 유지한다.
- 설정으로 HTTP broker publisher를 선택할 수 있게 한다.
- HTTP request method, path, headers, JSON envelope contract를 테스트한다.
- HTTP 2xx는 publish 성공, non-2xx/timeout/IO는 publish 실패로 relay retry/manual review 정책에 연결한다.

## 범위

- HTTP outbox publisher adapter 추가
- publisher type 설정 추가
- contract test 추가
- relay service 실패 연동 테스트 보강
- ADR, progress report, README, local guide 갱신

## 제외 범위

- Kafka/RabbitMQ/SQS client 도입
- schema registry
- consumer idempotency 구현
- DLQ topic/queue
- broker 인증/서명

## 완료 조건

- [x] 기본 publisher는 in-memory adapter로 유지된다.
- [x] `ai-repo.outbox.publisher.type=http` 설정 시 HTTP adapter가 활성화된다.
- [x] HTTP adapter가 POST로 JSON envelope를 발행한다.
- [x] envelope에 outboxEventId, operationId, eventType, aggregateType, aggregateId, occurredAt, payload가 포함된다.
- [x] HTTP 2xx 응답은 성공으로 처리한다.
- [x] HTTP non-2xx 응답은 실패로 처리한다.
- [x] relay service가 HTTP adapter 실패를 outbox FAILED로 연결한다.
- [x] `./gradlew test scenarioTest check`가 통과한다.

## 검증

```bash
./gradlew test scenarioTest check
```
