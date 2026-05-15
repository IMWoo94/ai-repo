# 0056. HTTP Publish Consume Loop

## 스펙 목표

Outbox relay가 HTTP publisher로 event를 발행하고, 실제 HTTP consumer endpoint가 이를 받아 receipt를 남기는 end-to-end loop 증거를 추가한다.

## 완료 결과

- `HttpOutboxPublishConsumeLoopTest`를 추가했다.
- Spring Boot random port 서버의 `/internal/broker/outbox-events`를 실제 consumer endpoint로 사용한다.
- 테스트 안에서 `HttpOperationOutboxPublisher` endpoint를 random port로 구성한다.
- 충전 → outbox 생성 → relay publish → HTTP consume → receipt 저장 → outbox `PUBLISHED` 상태를 검증한다.

## 검증

- `./gradlew test --tests "*HttpOutboxPublishConsumeLoopTest" --tests "*HttpOperationOutboxPublisherContractTest" --tests "*OperationOutboxConsumerControllerTest"`

## 남은 일

- broker-specific Testcontainers publish→consume loop
- local smoke에 HTTP consumer loop 추가
- pruning 실행 이력 저장

## 관련 문서

- `docs/adr/0046-http-publish-consume-loop.md`
- `docs/adr/0045-http-outbox-consumer-adapter.md`
- `docs/adr/0035-http-outbox-broker-adapter.md`
