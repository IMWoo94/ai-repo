# ADR-0032: Operational Log Pruning

## 상태

Accepted

## 맥락

Outbox relay run과 admin API access audit은 운영 상태 확인과 장애 분석을 위해 구조화된 데이터로 저장된다. 하지만 보존 기간이 없으면 운영 관측 로그가 계속 증가하고, 최근 이력 조회 API도 장기 데이터와 섞일 수 있다.

금융/핀테크 도메인에서 모든 기록을 같은 방식으로 삭제하면 안 된다. 거래, 원장, 도메인 감사 이벤트, requeue 감사 이력은 돈 이동과 운영 조치의 근거이므로 이번 pruning 대상에서 제외한다. 이번 결정은 scheduler 실행 이력과 운영 API 접근 이력처럼 관측 목적이 강한 운영 로그에만 적용한다.

## 선택지

### 선택지 A: 보존 기간 없이 계속 저장한다

장점:

- 삭제 위험이 없다.
- 구현 변경이 없다.
- 모든 과거 운영 로그를 로컬 DB에 계속 남길 수 있다.

단점:

- 운영 로그가 무기한 증가한다.
- 최근 이력 조회 API가 장기 데이터와 섞인다.
- 운영 데이터 보존 정책을 코드와 문서로 설명하기 어렵다.

### 선택지 B: 설정 기반 보존 기간과 pruning service를 둔다

장점:

- Relay run과 admin access audit에 서로 다른 보존 기간을 적용할 수 있다.
- 인메모리와 PostgreSQL 저장소에서 같은 삭제 경계를 테스트할 수 있다.
- 수동 운영 API와 scheduler를 같은 service에 연결할 수 있다.
- 기본 scheduler를 비활성화해 로컬 수동 검증에 간섭하지 않는다.

단점:

- delete repository 메서드와 pruning API가 추가된다.
- 잘못된 보존 기간 설정은 필요한 운영 로그를 일찍 삭제할 수 있다.
- 별도 archive storage는 아직 없다.

### 선택지 C: archive storage를 먼저 도입한다

장점:

- 삭제 전 장기 보관소로 이관할 수 있다.
- 실제 금융권 감사 보존 요구에 더 가깝다.

단점:

- 현재 로컬 포트폴리오 단계에서는 저장소, 배치, 검증 범위가 과하다.
- 아직 법적 보존 기간과 외부 저장소 정책이 확정되지 않았다.

## 결정

설정 기반 보존 기간과 pruning service를 둔다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 대상 | relay run, admin API access audit |
| 제외 | transaction, ledger, audit_events, requeue audit |
| relay run 기본 보존 | 30일 |
| admin access audit 기본 보존 | 180일 |
| 수동 실행 API | `POST /api/v1/operational-log-pruning-runs` |
| 권한 | `X-Admin-Token`, `X-Operator-Id` 필요 |
| 자동 실행 | scheduler bean, 기본 비활성화 |
| cutoff 조건 | timestamp `< cutoff` |
| 설정 prefix | `ai-repo.operational-log-pruning` |

## 결과

장점:

- 운영 관측 로그의 증가를 제어할 수 있다.
- 보존 기간이 코드, 설정, 문서에 명시된다.
- 삭제 대상과 제외 대상의 경계가 ADR로 남는다.
- 수동 API와 scheduler가 같은 service를 사용하므로 테스트 범위가 작다.

비용:

- archive storage 없이 삭제만 수행한다.
- 법적 보존 기간은 아직 학습용 기본값이다.
- pruning 실행 자체의 별도 이력은 아직 없다.

후속 작업:

- pruning 실행 이력을 별도 operational log로 남길지 결정한다.
- 운영 화면에서 pruning 결과를 확인할 수 있게 한다.
- Spring Security role 모델 도입 시 pruning API를 더 높은 권한으로 분리한다.
