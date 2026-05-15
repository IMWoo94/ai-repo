# 0057. Consumer Monitoring Admin API

## 스펙 목표

HTTP consumer가 처리한 event, duplicate no-op, receipt side effect를 운영자가 조회할 수 있는 API와 검증 근거를 추가한다.

## 완료 결과

- `operation_outbox_consumer_processed_events.duplicate_count` migration을 추가했다.
- duplicate event 수신 시 기존 processed-event row의 `duplicate_count`를 증가시킨다.
- `GET /api/v1/outbox-consumer/metrics` API를 추가했다.
- `GET /api/v1/outbox-consumer/receipts?limit=50` API를 추가했다.
- 신규 API를 Spring Security operator 권한과 admin access audit 대상에 포함했다.

## 검증

- `./gradlew test --tests "*OperationOutboxConsumerMonitoringControllerTest" --tests "*JdbcWalletRepositoryTest" --tests "*OperationOutboxConsumerServiceTest"`

## 남은 일

- broker replay window별 retention 권장값
- duplicate spike alert summary
- broker-specific adapter와 consumer metric 연결

## 관련 문서

- `docs/adr/0047-consumer-monitoring-admin-api.md`
- `docs/releases/unreleased.md`
- `wiki-drafts/QA-Scenarios.md`
