# 0031. Operational Log Pruning

## 스펙 목표

- Relay run과 admin API access audit에 보존 기간을 적용한다.
- cutoff 이전 운영 관측 로그를 삭제하는 service를 추가한다.
- 수동 운영 API와 비활성 기본 scheduler로 pruning을 실행할 수 있게 한다.

## 완료 결과

- `OperationalLogPruningPolicy`, `OperationalLogPruningService`, `OperationalLogPruningResult`를 추가했다.
- Relay run 기본 보존 기간은 30일, admin access audit 기본 보존 기간은 180일로 설정했다.
- 인메모리 저장소와 JDBC 저장소에 cutoff 이전 삭제 메서드를 추가했다.
- `POST /api/v1/operational-log-pruning-runs` 운영 API를 추가했다.
- pruning API도 admin access audit filter 대상에 포함했다.
- `OperationalLogPruningScheduler`를 추가하고 기본 비활성화로 설정했다.
- 단위/API/scheduler/JDBC 테스트로 cutoff, 권한, 저장소 삭제를 검증했다.

## 검증

- `./gradlew test --tests '*OperationalLogPruningServiceTest' --tests '*OperationalLogPruningSchedulerTest' --tests '*OperationalLogPruningControllerTest' --tests '*JdbcWalletRepositoryTest'`
- `./gradlew test scenarioTest check`
- `git diff --check`

## 남은 일

- pruning 실행 이력 자체를 별도 로그로 남길지 결정한다.
- archive storage 이관 여부를 검토한다.
- Spring Security role 모델 도입 시 pruning API 권한을 분리한다.

## 관련 문서

- `docs/adr/0032-operational-log-pruning.md`
- `docs/development/local-setup.md`
- `issue-drafts/0031-operational-log-pruning.md`
