# Consumer Duplicate Spike Alert

## 배경

Consumer duplicate count는 조회할 수 있지만, 운영자가 숫자를 직접 해석해야 한다. Duplicate delivery가 급증하면 broker replay나 relay/consumer 장애 복구 이상 징후일 수 있다.

## 목표

- consumer health summary API를 추가한다.
- duplicate rate 기준으로 `OK`, `WARNING`, `CRITICAL`, `NO_DATA`를 반환한다.
- threshold는 설정으로 조정한다.
- operator 권한으로 조회 가능하게 한다.

## 완료 조건

- `GET /api/v1/outbox-consumer/health`가 health summary를 반환한다.
- warning/critical/no data 판정 테스트가 있다.
- API 권한 테스트가 있다.
- ADR/progress/Wiki/release 문서가 갱신된다.

## 검증 명령

```bash
./gradlew test --tests "*OperationOutboxConsumerMonitoringServiceTest" --tests "*OperationOutboxConsumerMonitoringControllerTest" --tests "*OperationOutboxConsumerServiceTest"
```
