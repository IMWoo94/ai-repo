# Outbox Publisher Port

## 배경

현재 outbox relay는 event를 claim하고 `PUBLISHED` 또는 `FAILED`로 상태 전이할 수 있다. 하지만 실제 발행 행위는 코드 경계로 분리되어 있지 않다.

금융/핀테크 흐름에서 outbox는 외부 브로커를 붙이기 전에도 다음 질문에 답할 수 있어야 한다.

- relay가 어떤 event를 발행 대상으로 잡았는가?
- 발행 성공과 실패가 outbox 상태 전이로 연결되는가?
- 실제 broker를 붙이기 전에도 테스트 가능한 발행 port가 있는가?
- 발행 실패가 retry와 manual review 정책으로 이어지는가?

## 목표

- `OperationOutboxPublisher` port를 추가한다.
- 초기 구현은 외부 broker 대신 fake/in-memory publisher adapter를 둔다.
- relay service가 claim → publish → mark published/failed 흐름을 한 번에 실행하는 API를 제공한다.
- 성공/실패 publisher 테스트로 outbox 상태 전이와 retry 정책을 검증한다.

## 범위

- 애플리케이션 port/interface 추가
- fake publisher adapter 추가
- relay publish batch orchestration 추가
- 단위 테스트와 시나리오 테스트 보강
- ADR, progress report, 테스트 문서 갱신

## 제외 범위

- Kafka, RabbitMQ, SQS 같은 실제 broker 연동
- scheduler 또는 background worker 기동
- 메시지 consumer와 멱등 consumer 구현
- 운영자 승인 workflow

## 완료 조건

- [ ] publisher port가 application layer에 정의된다.
- [ ] 기본 로컬 프로필에서 fake publisher가 주입된다.
- [ ] relay service가 claim한 event를 publisher로 발행하고 성공 시 `PUBLISHED`로 전이한다.
- [ ] publisher 실패 시 `FAILED` 또는 `MANUAL_REVIEW` 정책으로 연결된다.
- [ ] 테스트가 성공/실패/partial failure 흐름을 검증한다.
- [ ] ADR과 progress report가 추가된다.
- [ ] `./gradlew test scenarioTest check`가 통과한다.

## 검증

```bash
./gradlew test scenarioTest check
```
