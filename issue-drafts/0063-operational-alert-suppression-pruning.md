# Operational Alert Suppression and Pruning

## 배경

Operational alert record는 relay/consumer health의 warning·critical 판정을 저장하지만, health endpoint를 반복 조회하면 같은 alert가 계속 누적될 수 있다. 또한 alert table 보존 기간이 없어 장기 실행 시 운영 관측 로그가 무제한 증가한다.

## 목표

- 같은 alert 원인은 현재 alert의 직전 window 안에서 중복 저장하지 않는다.
- 오래된 alert record를 보존 기간 기준으로 삭제한다.
- 기존 operational log pruning API 결과에 alert pruning 결과를 포함한다.
- UI 작업 필수 참조 문서인 `DESIGN-apple.md`를 추가한다.

## 완료 조건

- [x] `OperationalAlertPolicy`가 suppression window와 retention 설정을 제공한다.
- [x] 같은 `source`, `severity`, `reasons` 조합은 suppression window 안에서 한 번만 저장된다.
- [x] 단일 애플리케이션 인스턴스의 동시 publish에서도 check/insert 경합으로 중복 저장되지 않는다.
- [x] 미래 timestamp alert는 과거 timestamp alert의 suppression 근거가 되지 않는다.
- [x] operational log pruning이 alert cutoff와 deleted count를 반환한다.
- [x] JDBC/InMemory repository가 duplicate lookup과 cutoff 삭제를 지원한다.
- [x] service/controller/scheduler/JDBC 테스트가 갱신된다.
- [x] ADR, progress, release, wiki draft가 갱신된다.

## 검증 명령

```bash
./gradlew test --tests "*OperationalAlertServiceTest" --tests "*OperationalLogPruningServiceTest" --tests "*OperationalLogPruningControllerTest" --tests "*OperationalLogPruningSchedulerTest" --tests "*JdbcWalletRepositoryTest"
scripts/check-dev-rules.sh
```
