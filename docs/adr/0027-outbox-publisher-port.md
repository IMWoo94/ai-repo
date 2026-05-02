# ADR-0027: Outbox Publisher Port

## 상태

Accepted

## 맥락

Transactional outbox는 돈 이동 트랜잭션 안에서 event를 안전하게 저장하고, relay 상태 전이와 retry/manual review 정책까지 갖추었다. 하지만 relay가 실제로 외부 메시지 시스템에 event를 발행하는 경계는 아직 코드로 분리되어 있지 않았다.

이 상태에서는 outbox event가 `PENDING`, `FAILED`, `PUBLISHED`로 바뀌는 정책은 검증할 수 있지만, 다음 질문에는 답하기 어렵다.

- relay가 claim한 event를 어떤 발행 경계로 넘기는가?
- broker 발행 실패가 retry와 manual review 정책으로 이어지는가?
- Kafka/RabbitMQ 같은 실제 broker를 붙이기 전에 테스트 가능한 port가 있는가?

## 선택지

### 선택지 A: relay 상태 전이만 유지한다

장점:

- 코드가 가장 단순하다.
- 외부 메시징 선택을 더 늦출 수 있다.

단점:

- outbox가 실제 발행 흐름과 연결되지 않는다.
- relay 성공/실패 테스트가 상태 조작 테스트에 머문다.
- 실제 broker 도입 시 application service 변경 폭이 커질 수 있다.

### 선택지 B: publisher port와 fake adapter를 먼저 둔다

장점:

- application layer는 `OperationOutboxPublisher` port에만 의존한다.
- 실제 broker 없이도 claim → publish → mark published/failed 흐름을 테스트할 수 있다.
- broker 선택을 미루면서도 구조적 확장 지점을 만든다.

단점:

- 아직 실제 네트워크 발행 보장은 없다.
- fake adapter의 성공은 broker 계약 검증을 대체하지 못한다.

### 선택지 C: 실제 broker adapter를 바로 붙인다

장점:

- 운영 구조에 가장 가깝다.
- 메시지 직렬화, topic/routing, broker 장애를 빠르게 검증할 수 있다.

단점:

- 현재 학습 단계에서는 Kafka/RabbitMQ/SQS 선택이 이르다.
- 로컬/CI 인프라가 무거워지고, outbox 정책 학습 범위가 흐려진다.

## 결정

publisher port와 fake adapter를 먼저 둔다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| Port | `OperationOutboxPublisher` |
| 기본 adapter | `InMemoryOperationOutboxPublisher` |
| Batch API | `OperationOutboxRelayService.publishReadyEvents(limit)` |
| 성공 처리 | publisher 성공 후 `PUBLISHED` 전이 |
| 실패 처리 | publisher 예외 메시지를 `lastError`로 저장하고 retry 정책 적용 |
| 결과 모델 | `OperationOutboxPublishBatchResult` |

## 결과

장점:

- outbox relay가 상태 전이만 하는 코드에서 실제 발행 경계를 호출하는 구조로 이동한다.
- 성공, 실패, partial failure를 단위 테스트로 검증할 수 있다.
- 실제 broker adapter는 port 구현체만 교체하는 방식으로 확장할 수 있다.

비용:

- fake adapter는 broker 계약, topic, serialization compatibility를 검증하지 못한다.
- scheduler/background worker는 아직 없으므로 운영 자동 실행은 후속 작업으로 남는다.

후속 작업:

- 실제 broker 후보를 정한 뒤 adapter와 contract test를 추가한다.
- relay scheduler 또는 worker 실행 정책을 분리 ADR로 결정한다.
- publisher payload schema version 정책을 추가한다.
