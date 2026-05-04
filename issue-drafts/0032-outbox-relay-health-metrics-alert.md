# Outbox Relay Health Metrics and Alert

## 배경

Outbox relay scheduler 실행 이력은 저장되고 최근 실행 목록도 조회할 수 있다. 하지만 운영자가 매번 실행 목록을 직접 해석해야 하며, 마지막 성공 시각, 연속 실패 횟수, 실패율 같은 핵심 상태 요약과 alert 판정은 없다.

금융/핀테크 운영에서는 자동 worker가 단순히 존재하는 것보다 실패가 누적되는지, 마지막 성공이 너무 오래됐는지, 최근 실패율이 높은지를 빠르게 판단할 수 있어야 한다.

## 목표

- Outbox relay run 이력 기반 health metrics summary를 제공한다.
- 최근 실행 window 안에서 total/success/failed count와 failure rate를 계산한다.
- 마지막 성공/실패 시각과 연속 실패 횟수를 계산한다.
- 설정 기반 alert threshold로 `OK`, `WARNING`, `CRITICAL`, `NO_DATA` 상태를 판정한다.
- 운영 API로 health summary를 조회한다.

## 범위

- relay health status/result/policy model 추가
- monitoring service summary 계산 추가
- 운영자 전용 health API 추가
- application.yml threshold 설정 추가
- 단위/API 테스트 추가
- ADR, progress report, README, local guide 갱신

## 제외 범위

- Prometheus/Micrometer actuator endpoint
- Slack/Email/PagerDuty 같은 외부 알림 발송
- 실제 broker adapter health check
- pruning 실행 이력과 결합

## 완료 조건

- [x] 실행 이력이 없으면 `NO_DATA`를 반환한다.
- [x] 최근 실행이 모두 정상이고 마지막 성공이 threshold 안이면 `OK`를 반환한다.
- [x] 연속 실패가 warning threshold 이상이면 `WARNING`을 반환한다.
- [x] 연속 실패가 critical threshold 이상이면 `CRITICAL`을 반환한다.
- [x] 마지막 성공이 너무 오래됐으면 `CRITICAL`을 반환한다.
- [x] 실패율이 warning threshold 이상이면 `WARNING`을 반환한다.
- [x] 운영 health API가 admin authz guard를 사용한다.
- [x] `./gradlew test scenarioTest check`가 통과한다.

## 검증

```bash
./gradlew test scenarioTest check
```
