# ADR-0035: HTTP Outbox Broker Adapter

## 상태

Accepted

## 맥락

Outbox relay는 `OperationOutboxPublisher` port와 in-memory adapter를 통해 claim → publish → published/failed 흐름을 검증해 왔다. 그러나 in-memory adapter는 실제 외부 I/O, HTTP status, timeout, serialization contract를 검증하지 못한다.

Kafka/RabbitMQ/SQS 같은 메시지 브로커를 바로 붙이면 로컬 개발과 CI 인프라 비용이 커진다. 현재 단계의 목표는 특정 제품 선택보다 “application port 뒤에 실제 외부 발행 adapter를 붙이고, 발행 contract를 테스트로 고정하는 것”이다.

## 선택지

### 선택지 A: in-memory adapter를 유지한다

장점:

- 테스트가 빠르고 안정적이다.
- 외부 프로세스나 네트워크가 필요 없다.
- 기존 relay 상태 전이 테스트가 단순하다.

단점:

- 실제 외부 I/O 실패를 검증하지 못한다.
- 발행 envelope, header, status code contract가 없다.
- fake adapter 성공은 실제 broker 연동 가능성을 보장하지 않는다.

### 선택지 B: HTTP broker adapter를 먼저 도입한다

장점:

- JDK `HttpClient`만으로 실제 네트워크 I/O를 검증할 수 있다.
- JDK HTTP server로 contract test를 작성할 수 있어 CI 인프라가 작다.
- 2xx/non-2xx 응답을 relay retry/manual review 정책과 연결할 수 있다.
- 향후 Kafka/RabbitMQ adapter도 같은 `OperationOutboxPublisher` port 뒤에 붙일 수 있다.

단점:

- Kafka/RabbitMQ/SQS 같은 durable queue semantics는 아직 없다.
- schema registry, partition key, consumer group 같은 메시지 브로커 고유 계약은 검증하지 않는다.
- broker 인증/서명은 아직 없다.

### 선택지 C: Kafka/RabbitMQ/SQS adapter를 바로 도입한다

장점:

- 실제 메시지 브로커 운영 모델에 더 가깝다.
- partition/routing, DLQ, retry topic 같은 메시징 기능을 빠르게 검토할 수 있다.

단점:

- 로컬/CI에 브로커 컨테이너와 client dependency가 필요하다.
- 현재 학습 단계에서는 broker 제품 선택 논의가 구현 범위를 크게 키운다.
- consumer idempotency와 schema compatibility까지 함께 다뤄야 한다.

## 결정

HTTP broker adapter를 먼저 도입한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| port | `OperationOutboxPublisher` |
| 기본 adapter | `InMemoryOperationOutboxPublisher` |
| 실제 I/O adapter | `HttpOperationOutboxPublisher` |
| adapter 선택 | `ai-repo.outbox.publisher.type` |
| 기본 type | `memory` |
| HTTP type | `http` |
| HTTP method | `POST` |
| content type | `application/json` |
| idempotency header | `X-Outbox-Event-Id` |
| 성공 기준 | HTTP 2xx |
| 실패 기준 | HTTP non-2xx, IO, interrupt |

## 결과

장점:

- 기본 로컬 실행은 기존처럼 in-memory publisher를 사용한다.
- 설정만 바꾸면 실제 HTTP broker endpoint로 outbox event를 발행할 수 있다.
- 발행 envelope와 header contract가 테스트로 고정된다.
- HTTP 실패가 relay 실패 처리와 연결된다.

비용:

- durable queue, DLQ, consumer group은 아직 없다.
- broker 인증/서명은 아직 없다.
- Kafka/RabbitMQ/SQS adapter는 후속 작업이다.

후속 작업:

- consumer idempotency key 정책을 설계한다.
- Kafka/RabbitMQ/SQS 중 하나를 선택해 product-specific adapter를 추가한다.
- broker 인증, 서명, retry topic/DLQ 정책을 별도 ADR로 결정한다.
