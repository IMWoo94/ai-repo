# 0032. Outbox Relay Health Metrics and Alert

## 스펙 목표

- Relay scheduler 실행 이력에서 운영 health summary를 계산한다.
- 마지막 성공/실패 시각, 실패율, 연속 실패 횟수를 제공한다.
- 설정 기반 threshold로 `OK`, `WARNING`, `CRITICAL`, `NO_DATA`를 판정한다.

## 완료 결과

- `OutboxRelayHealthPolicy`, `OutboxRelayHealthStatus`, `OutboxRelayHealthSummary`를 추가했다.
- `OperationOutboxRelayMonitoringService`에 health summary 계산을 추가했다.
- 기본 threshold를 application 설정에 추가했다.
- `GET /api/v1/outbox-relay-runs/health` 운영 API를 추가했다.
- 단위/API 테스트로 no data, OK, warning, critical, stale success, 권한을 검증했다.

## 검증

- `./gradlew test --tests '*OperationOutboxRelayMonitoringServiceTest' --tests '*OperationOutboxRelayRunControllerTest' --tests '*OperationOutboxRelaySchedulerTest'`
- `./gradlew test scenarioTest check`
- `git diff --check`

## 남은 일

- Micrometer/Actuator metric endpoint를 추가한다.
- 외부 alert channel과 escalation policy를 설계한다.
- 실제 broker adapter 도입 후 publish 실패와 health 판정을 연결 검증한다.

## 관련 문서

- `docs/adr/0033-outbox-relay-health-metrics-alert.md`
- `docs/development/local-setup.md`
- `issue-drafts/0032-outbox-relay-health-metrics-alert.md`
