# Operational Alert Record Channel

## 배경

Relay/consumer health는 alert 판정을 반환하지만, 운영자가 endpoint를 직접 조회하지 않으면 alert 이력이 남지 않는다.

## 목표

- warning/critical health summary를 운영 alert record로 저장한다.
- 운영자가 최근 alert를 조회할 수 있게 한다.
- Slack/Webhook 도입 전 로컬 검증 가능한 alert channel 계약을 만든다.

## 완료 조건

- [x] `operational_alerts` migration이 존재한다.
- [x] relay/consumer warning·critical health 평가 시 alert가 저장된다.
- [x] `OK`, `NO_DATA`는 alert를 저장하지 않는다.
- [x] `GET /api/v1/operational-alerts?limit=50`가 operator 권한으로 최근 alert를 반환한다.
- [x] API 접근은 admin access audit 대상이다.
- [x] service/API/JDBC 테스트가 존재한다.

## 검증 명령

```bash
./gradlew test --tests "*OperationOutboxConsumerMonitoringServiceTest" --tests "*OperationOutboxRelayMonitoringServiceTest" --tests "*OperationalAlertControllerTest" --tests "*JdbcWalletRepositoryTest"
```
