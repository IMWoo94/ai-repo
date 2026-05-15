# 0062. Operational Alert Record Channel

## 스펙 목표

Relay/consumer health의 warning·critical 판정이 단순 응답에만 머물지 않고 운영 alert record로 남도록 한다.

## 완료 결과

- `operational_alerts` 테이블과 `OperationalAlert` 도메인 record를 추가했다.
- `OperationalAlertService`와 repository port를 추가했다.
- relay/consumer health summary가 `WARNING`, `CRITICAL`이면 alert record를 저장한다.
- `GET /api/v1/operational-alerts?limit=50` 조회 API를 추가했다.
- 운영 alert API를 operator/admin token 보호 및 admin access audit 대상에 포함했다.

## 검증

- `./gradlew test --tests "*OperationOutboxConsumerMonitoringServiceTest" --tests "*OperationOutboxRelayMonitoringServiceTest" --tests "*OperationalAlertControllerTest" --tests "*JdbcWalletRepositoryTest"`

## 남은 일

- alert dedupe/suppression window
- alert record pruning
- Slack/Webhook adapter와 contract test

## 관련 문서

- `docs/adr/0052-operational-alert-record-channel.md`
- `docs/releases/unreleased.md`
- `wiki-drafts/QA-Scenarios.md`
