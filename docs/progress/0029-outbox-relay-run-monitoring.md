# 0029. Outbox Relay Run Monitoring

## 스펙 목표

- Outbox relay scheduler 실행 결과를 구조화된 이력으로 남긴다.
- 성공/실패 실행을 구분하고 처리 건수와 오류 메시지를 저장한다.
- 운영 API로 최근 scheduler 실행 이력을 조회한다.

## 완료 결과

- `OperationOutboxRelayRun`과 `OperationOutboxRelayRunStatus`를 추가했다.
- `OperationOutboxRelayMonitoringService`를 추가했다.
- scheduler가 성공 실행에는 claimed/published/failed count를 기록한다.
- scheduler가 실패 실행에는 오류 메시지를 기록한 뒤 예외를 다시 던진다.
- 실패 오류 메시지는 DB 저장 가능하도록 255자로 제한한다.
- 인메모리 저장소와 JDBC 저장소에 relay run 저장/조회 기능을 추가했다.
- PostgreSQL Flyway `V9__create_outbox_relay_runs.sql`을 추가했다.
- `GET /api/v1/outbox-relay-runs` 운영 API를 추가했다.
- 단위/API/JDBC 테스트로 성공, 실패, 권한, 저장소 조회를 검증했다.

## 검증

- `./gradlew test --tests '*OperationOutboxRelaySchedulerTest' --tests '*OperationOutboxRelayMonitoringServiceTest' --tests '*OperationOutboxRelayRunControllerTest' --tests '*JdbcWalletRepositoryTest'`

## 남은 일

- 실행 이력 보존 기간과 pruning 정책을 추가한다.
- metric/alert backend를 추가한다.
- 실제 broker adapter 도입 후 relay run과 publish 결과를 연결 검증한다.

## 관련 문서

- `docs/adr/0030-outbox-relay-run-monitoring.md`
- `docs/development/local-setup.md`
- `issue-drafts/0029-outbox-relay-run-monitoring.md`
