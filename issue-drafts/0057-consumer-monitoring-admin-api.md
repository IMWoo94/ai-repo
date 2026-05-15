# Consumer Monitoring Admin API

## 배경

HTTP publish→consume loop는 자동 테스트로 검증되지만, 운영자는 consumer 처리량, duplicate no-op, 최근 receipt를 조회할 수 있어야 한다.

## 목표

- consumer processed count, duplicate count, receipt count를 조회한다.
- 최근 consumer receipt를 조회한다.
- 운영 조회 API는 operator/admin token과 operator id로 보호한다.
- duplicate 수신은 side effect 없이 `duplicate_count`만 증가시킨다.

## 완료 조건

- `GET /api/v1/outbox-consumer/metrics`가 지표를 반환한다.
- `GET /api/v1/outbox-consumer/receipts?limit=50`가 최근 receipt를 반환한다.
- 인증 없는 요청은 401을 반환한다.
- JDBC repository test와 controller test가 통과한다.

## 검증 명령

```bash
./gradlew test --tests "*OperationOutboxConsumerMonitoringControllerTest" --tests "*JdbcWalletRepositoryTest" --tests "*OperationOutboxConsumerServiceTest"
```
