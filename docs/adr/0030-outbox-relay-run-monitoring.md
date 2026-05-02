# ADR-0030: Outbox Relay Run Monitoring

## 상태

Accepted

## 맥락

Outbox relay scheduler는 애플리케이션 안에서 주기적으로 `publishReadyEvents(batchSize)`를 실행할 수 있다. 그러나 scheduler가 실행되었는지, 몇 건을 claim/publish/fail 했는지, 실행 자체가 실패했는지 확인할 기록이 없었다.

금융/핀테크 운영에서는 자동 worker가 존재하는 것보다 “언제 어떤 결과로 실행되었는가”가 더 중요하다. 장애가 발생했을 때 마지막 성공 시각, 실패 메시지, 처리 건수를 빠르게 확인할 수 있어야 한다.

## 선택지

### 선택지 A: 로그에만 남긴다

장점:

- 구현이 가장 작다.
- 별도 테이블이나 조회 API가 필요 없다.

단점:

- 운영자가 API로 최근 실행 상태를 확인할 수 없다.
- 로그 보존/검색 환경이 없으면 근거가 사라진다.
- 테스트에서 실행 이력을 구조적으로 검증하기 어렵다.

### 선택지 B: relay run 이력을 저장하고 운영 API로 조회한다

장점:

- scheduler 실행 결과가 구조화된 데이터로 남는다.
- 성공/실패, 처리 건수, 오류 메시지를 테스트와 API로 검증할 수 있다.
- PostgreSQL profile에서도 운영 확인 근거가 보존된다.

단점:

- 실행 이력 테이블과 repository가 추가된다.
- 오래된 실행 이력 보존/삭제 정책은 아직 별도 결정이 필요하다.

### 선택지 C: metric backend를 바로 붙인다

장점:

- 운영 환경에 가까운 모니터링과 알림을 설계할 수 있다.
- dashboard와 alert rule로 확장하기 좋다.

단점:

- 현재 로컬 포트폴리오 단계에서는 Prometheus/Grafana 같은 운영 인프라가 과하다.
- metric만으로는 개별 실행 이력과 오류 메시지 조회가 부족할 수 있다.

## 결정

relay run 이력을 저장하고 운영 API로 조회한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 이력 모델 | `OperationOutboxRelayRun` |
| 상태 | `SUCCESS`, `FAILED` |
| 저장 필드 | startedAt, completedAt, batchSize, claimed/published/failed count, errorMessage |
| 조회 API | `GET /api/v1/outbox-relay-runs?limit=50` |
| 권한 | `X-Admin-Token`, `X-Operator-Id` 필요 |
| PostgreSQL 테이블 | `operation_outbox_relay_runs` |
| Flyway migration | `V9__create_outbox_relay_runs.sql` |
| 오류 메시지 | 255자까지 저장 |

## 결과

장점:

- scheduler 실행 결과가 인메모리와 PostgreSQL 모두에 남는다.
- 운영 API로 최근 실행 이력을 확인할 수 있다.
- 성공 실행과 실패 실행이 테스트로 분리 검증된다.

비용:

- 실행 이력 보존 기간과 pruning 정책은 아직 없다.
- metric/alert backend와 dashboard는 아직 없다.
- scheduler가 비활성화된 기본 로컬 환경에서는 실행 이력이 자동 생성되지 않는다.

후속 작업:

- 실행 이력 보존 기간과 삭제 배치를 설계한다.
- 실패율, 마지막 성공 시각, 연속 실패 횟수 metric을 추가한다.
- 실제 broker adapter 도입 후 relay run과 broker publish 결과를 함께 검증한다.
