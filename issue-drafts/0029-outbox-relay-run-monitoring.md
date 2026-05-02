# Outbox Relay Run Monitoring

## 배경

Outbox relay scheduler는 주기적으로 `publishReadyEvents(batchSize)`를 실행할 수 있다. 하지만 현재는 scheduler가 실행되었는지, 몇 건을 claim/publish/fail 했는지, 실행 자체가 실패했는지 확인할 방법이 없다.

금융/핀테크 운영에서는 자동 worker가 존재하는 것만으로 충분하지 않다. 각 실행이 기록되고, 최근 실행 상태를 조회할 수 있어야 장애 원인 분석과 운영 확인이 가능하다.

## 목표

- Outbox relay scheduler 실행 이력을 기록한다.
- 성공 실행에는 batch size, claimed, published, failed count를 남긴다.
- 실패 실행에는 오류 메시지를 남긴다.
- 운영 API로 최근 relay 실행 이력을 조회한다.
- 인메모리와 PostgreSQL profile 모두 같은 조회/저장 경계를 갖는다.

## 범위

- relay run domain model 추가
- relay run repository/service 추가
- scheduler 실행 전후 기록
- relay run 조회 운영 API 추가
- PostgreSQL Flyway migration 추가
- 단위/API/JDBC 테스트 추가
- ADR, progress report, README 갱신

## 제외 범위

- metric backend 연동
- alert rule
- Grafana/Prometheus dashboard
- distributed scheduler singleton 보장

## 완료 조건

- [ ] scheduler 성공 실행 이력이 저장된다.
- [ ] scheduler 실패 실행 이력이 저장된다.
- [ ] 운영 API로 최근 실행 이력을 조회할 수 있다.
- [ ] 운영 API는 admin authz guard를 사용한다.
- [ ] PostgreSQL migration이 추가된다.
- [ ] `./gradlew test scenarioTest check`가 통과한다.

## 검증

```bash
./gradlew test scenarioTest check
```
