# 0026. Outbox Publisher Port

## 스펙 목표

- outbox relay가 상태 전이만 하지 않고 발행 port를 거치도록 만든다.
- 실제 broker 도입 전에도 claim → publish → published/failed 흐름을 테스트 가능하게 한다.
- 발행 실패가 retry/manual review 정책으로 이어지는지 검증한다.

## 완료 결과

- `OperationOutboxPublisher` application port를 추가했다.
- 기본 adapter로 `InMemoryOperationOutboxPublisher`를 추가했다.
- `OperationOutboxRelayService.publishReadyEvents(limit)` batch API를 추가했다.
- batch 결과 모델 `OperationOutboxPublishBatchResult`를 추가했다.
- publisher 성공 시 outbox event가 `PUBLISHED`로 전이된다.
- publisher 실패 시 `FAILED`로 전이되고 `nextRetryAt`, `lastError`가 기록된다.
- partial failure 테스트로 일부 성공/일부 실패 흐름을 검증했다.
- 시나리오 테스트에 outbox publish 흐름을 추가했다.

## 검증

- `./gradlew test --tests '*OperationOutboxRelayServiceTest' --tests '*WalletScenarioFlowTest'`

## 남은 일

- 실제 broker adapter와 contract test를 추가한다.
- relay scheduler/background worker 실행 정책을 결정한다.
- payload schema version과 consumer 멱등성 정책을 별도 ADR로 다룬다.

## 관련 문서

- `docs/adr/0027-outbox-publisher-port.md`
- `docs/testing/scenario-test-strategy.md`
- `issue-drafts/0026-outbox-publisher-port.md`
