# Consumer Processed Event Dedupe

## 배경

HTTP broker envelope는 `idempotencyKey`를 포함하지만, 실제 consumer가 이 key를 저장하지 않으면 중복 수신을 막을 수 없다.

Outbox relay는 최소 1회 발행 모델이므로 같은 event가 중복 전달될 수 있다. 금융 흐름에서는 중복 side effect가 원장, 알림, 정산, 감사 데이터 문제로 이어질 수 있다.

## 목표

- consumer processed-event 저장소를 추가한다.
- `idempotencyKey`를 unique 처리 기준으로 고정한다.
- duplicate 기록은 실패가 아니라 이미 처리된 메시지로 판단할 수 있게 한다.
- H2와 PostgreSQL 동시성 테스트로 검증한다.

## 완료 기준

- [x] processed-event domain record가 있다.
- [x] processed-event repository port가 있다.
- [x] Flyway migration이 있다.
- [x] H2 schema baseline이 갱신된다.
- [x] JDBC/in-memory repository 구현이 있다.
- [x] duplicate 기록은 `false`를 반환한다.
- [x] PostgreSQL 동시성 테스트에서 하나만 기록된다.

## 제외

- Kafka/RabbitMQ/SQS broker-specific consumer adapter
- broker replay window별 retention 권장값
- 외부 시스템 side effect transaction 통합
