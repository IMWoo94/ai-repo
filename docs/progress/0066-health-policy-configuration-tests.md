# 0066 Health Policy Configuration Tests

## 스펙 목표

- Outbox relay health policy의 설정값 변환과 guard rail을 단위 테스트로 고정한다.
- Outbox consumer duplicate health policy의 설정값 변환과 guard rail을 단위 테스트로 고정한다.
- 운영 alert/health threshold 변경 시 잘못된 설정이 조기에 실패하도록 회귀 기준을 보강한다.

## 완료 결과

- `OutboxRelayHealthPolicyTest`를 추가해 sample size, 연속 실패 임계값, failure rate percent, last success age minute 설정을 검증했다.
- `OperationOutboxConsumerHealthPolicyTest`를 추가해 duplicate count, warning/critical duplicate rate percent, window minute 설정을 검증했다.
- 음수/0/상한 초과/임계값 역전 설정이 명확한 예외 메시지로 거부되는지 확인했다.

## 검증

- `./gradlew test --tests '*HealthPolicyTest'`
- `./gradlew test`
- `scripts/check-dev-rules.sh`

## 남은 일

- 실제 운영 환경별 threshold 튜닝값은 관측 데이터가 쌓인 뒤 별도 ADR 또는 운영 가이드에서 조정한다.
