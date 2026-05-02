# Admin API Access Audit

## 배경

Outbox 운영 API는 admin token과 operator header로 보호되고, requeue 행위와 relay run 결과도 기록된다. 하지만 운영 API 자체를 누가 언제 호출했는지에 대한 접근 감사 로그는 없다.

금융/핀테크 운영에서는 변경 행위뿐 아니라 조회 행위도 감사 대상이다. manual review 조회, relay run 조회, requeue audit 조회처럼 상태를 확인하는 API도 접근 이력이 남아야 운영자 책임 추적과 사고 분석이 가능하다.

## 목표

- 운영 API 접근 감사 로그를 저장한다.
- 성공/실패 호출 모두 method, path, status code, operator header, occurredAt을 기록한다.
- admin token 값은 저장하지 않는다.
- 운영 API로 최근 접근 감사 로그를 조회한다.
- 인메모리와 PostgreSQL profile 모두 같은 저장/조회 경계를 갖는다.

## 범위

- admin API access audit domain model 추가
- repository/service 추가
- HTTP filter 기반 운영 API 접근 기록
- 운영 접근 감사 조회 API 추가
- PostgreSQL Flyway migration 추가
- 단위/API/JDBC 테스트 추가
- ADR, progress report, README 갱신

## 제외 범위

- IP 주소/User-Agent 저장
- 별도 SIEM 연동
- 감사 로그 보존 기간/pruning
- Spring Security principal 연동

## 완료 조건

- [x] 운영 API 호출 성공이 접근 감사 로그로 저장된다.
- [x] 운영 API 호출 실패가 접근 감사 로그로 저장된다.
- [x] admin token은 저장되지 않는다.
- [x] 운영 API로 최근 접근 감사 로그를 조회할 수 있다.
- [x] 운영 접근 감사 조회 API도 admin authz guard를 사용한다.
- [x] PostgreSQL migration이 추가된다.
- [x] `./gradlew test scenarioTest check`가 통과한다.

## 검증

```bash
./gradlew test scenarioTest check
```
