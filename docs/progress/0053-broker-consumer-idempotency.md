# 0053. Broker Consumer Idempotency

## 스펙 목표

Outbox event가 broker로 중복 발행될 수 있는 현실을 인정하고, consumer가 같은 event를 한 번만 처리할 수 있는 idempotency 계약을 고정한다.

## 완료 결과

- HTTP broker adapter에 `X-Idempotency-Key` header를 추가했다.
- HTTP broker adapter에 `X-Event-Schema-Version`, `X-Event-Type` header를 추가했다.
- 발행 envelope에 `schemaVersion`, `idempotencyKey`를 추가했다.
- `idempotencyKey`는 `outboxEventId`와 동일하게 정의했다.
- contract test가 header와 body의 idempotency 계약을 검증한다.
- ADR/Wiki/release candidate 문서에 consumer dedupe 기준을 기록했다.

## 검증

- `./gradlew test --tests "*HttpOperationOutboxPublisherContractTest" --tests "*OperationOutboxRelayServiceTest"`
- `scripts/check-dev-rules.sh`
- `./gradlew check`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `git diff --check`

## 남은 일

- 실제 consumer processed-event table과 unique constraint 구현
- broker별 partition/routing key contract test
- dedupe 보관 기간과 replay 운영 정책

## 관련 문서

- `docs/adr/0043-broker-consumer-idempotency.md`
- `docs/adr/0035-http-outbox-broker-adapter.md`
- `docs/adr/0042-outbox-claim-guarded-result-update.md`
