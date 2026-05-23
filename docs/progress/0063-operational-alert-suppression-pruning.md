# 0063. Operational Alert Suppression and Pruning

## 스펙 목표

Operational alert record가 health endpoint 조회 빈도에 비례해 무한히 증가하지 않도록 중복 억제와 보존 기간 기반 pruning을 추가한다.

## 완료 결과

- `OperationalAlertPolicy`를 추가해 suppression window와 retention을 설정 가능하게 했다.
- 같은 `source`, `severity`, `reasons` 조합의 alert는 현재 alert의 직전 suppression window 안에서 중복 저장하지 않는다.
- 단일 애플리케이션 인스턴스의 동시 publish를 직렬화해 check/insert 경합을 막았다.
- Operational log pruning에 alert cutoff와 deleted count를 포함했다.
- JDBC/InMemory repository에 alert duplicate lookup과 cutoff 삭제를 추가했다.
- `DESIGN-apple.md`를 추가해 UI 작업 필수 참조 문서 누락을 해소했다.

## 검증

- `./gradlew test --tests "*OperationalAlertServiceTest" --tests "*OperationalLogPruningServiceTest" --tests "*OperationalLogPruningControllerTest" --tests "*OperationalLogPruningSchedulerTest" --tests "*JdbcWalletRepositoryTest"`

## 남은 일

- Slack/Webhook adapter와 contract test
- pruning 실행 이력 저장과 조회 API
- 운영 alert 화면 연결
- 다중 인스턴스 환경의 DB 원자 suppression 정책

## 관련 문서

- `docs/adr/0053-operational-alert-suppression-pruning.md`
- `docs/releases/unreleased.md`
- `wiki-drafts/QA-Scenarios.md`
