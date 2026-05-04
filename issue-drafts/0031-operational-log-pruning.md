# Operational Log Pruning

## 배경

Outbox relay run과 admin API access audit은 운영 관측과 장애 분석을 위해 계속 저장된다. 하지만 보존 기간 없이 계속 쌓이면 로컬/운영 DB 크기가 증가하고, 최근 상태 조회 API도 오래된 데이터와 섞일 수 있다.

금융/핀테크 도메인에서 원장, 거래, 도메인 감사 로그는 임의 삭제 대상이 아니다. 이번 pruning은 돈 이동의 법적/회계 근거가 아니라 운영 관측 로그인 relay 실행 이력과 운영 API 접근 이력만 대상으로 한다.

## 목표

- Relay run 보존 기간 정책을 추가한다.
- Admin API access audit 보존 기간 정책을 추가한다.
- 두 운영 로그를 cutoff 이전 기준으로 삭제하는 application service를 추가한다.
- 인메모리와 PostgreSQL profile 모두 같은 pruning 경계를 갖는다.
- 운영 API로 수동 pruning을 실행할 수 있게 한다.
- 자동 pruning scheduler는 기본 비활성화로 둔다.

## 범위

- operational log pruning service/result 추가
- repository delete-before 메서드 추가
- in-memory/JDBC 저장소 pruning 구현
- 운영자 전용 pruning API 추가
- disabled-by-default pruning scheduler 추가
- application.yml 설정 추가
- 단위/API/JDBC/scheduler 테스트 추가
- ADR, progress report, README, local guide 갱신

## 제외 범위

- ledger, transaction, audit_events, requeue audit 삭제
- 법적 보존 기간 최종 확정
- 별도 archive storage 이관
- metric/alert 구현
- Spring Security role 모델

## 완료 조건

- [x] cutoff 이전 relay run이 삭제된다.
- [x] cutoff 이후 relay run은 유지된다.
- [x] cutoff 이전 admin API access audit이 삭제된다.
- [x] cutoff 이후 admin API access audit은 유지된다.
- [x] 운영 pruning API가 admin authz guard를 사용한다.
- [x] pruning scheduler는 기본 비활성화이며 설정으로 켤 수 있다.
- [x] PostgreSQL repository pruning이 테스트된다.
- [x] `./gradlew test scenarioTest check`가 통과한다.

## 검증

```bash
./gradlew test scenarioTest check
```
