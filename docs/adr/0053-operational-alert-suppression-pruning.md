# ADR-0053: Operational Alert Suppression and Pruning

## 상태

Accepted

## 배경

ADR-0052는 relay/consumer health의 `WARNING`, `CRITICAL` 판정을 `operational_alerts` record로 남기도록 결정했다. 이 방식은 로컬 운영 이력을 만들지만, health endpoint를 반복 조회하면 같은 source, severity, reason 조합의 alert가 계속 쌓인다.

또한 alert record는 운영 관측 로그이므로 장기 보존이 필요하지 않은 로컬 MVP 환경에서는 보존 기간 기준 삭제 경계가 필요하다.

## 결정

Operational alert에 suppression window와 retention policy를 추가한다.

| 항목 | 결정 |
| --- | --- |
| suppression 기준 | 같은 `source`, `severity`, `reasons` 조합이 현재 alert의 직전 window 안에 이미 있는 경우 |
| suppression window 기본값 | 15분 |
| retention 기본값 | 30일 |
| 설정 key | `ai-repo.operational-alert.suppression-window-minutes`, `ai-repo.operational-alert.retention-days` |
| pruning 경계 | 기존 `POST /api/v1/operational-log-pruning-runs`에 alert pruning 결과를 포함 |
| 삭제 기준 | `occurred_at < cutoff` |

## 트레이드오프

### 장점

- 운영자가 health endpoint를 새로고침해도 같은 alert가 짧은 시간 안에 중복 저장되지 않는다.
- alert table의 무제한 증가를 막는다.
- 기존 operational log pruning API와 scheduler를 재사용해 운영 API 표면을 늘리지 않는다.
- 단일 애플리케이션 인스턴스 안에서는 alert publish 경계를 직렬화해 check/insert 경합을 막는다.

### 비용

- 같은 원인이 suppression window 안에서 계속 발생해도 row는 하나만 남는다.
- suppression 기준이 문자열 `reasons` 비교이므로 reason 표현이 바뀌면 다른 alert로 저장된다.
- 다중 애플리케이션 인스턴스까지 포함한 DB 원자 suppression은 별도 유니크 버킷 또는 advisory lock 결정이 필요하다.
- pruning 실행 이력 자체를 장기 감사 record로 저장하는 기능은 아직 없다.

## 검증 기준

- 같은 source/severity/reasons alert는 15분 window 안에서 한 번만 저장된다.
- 현재 alert보다 미래에 발생한 alert는 과거 alert의 suppression 근거가 되지 않는다.
- 단일 애플리케이션 인스턴스의 동시 publish에서도 같은 alert는 하나만 저장된다.
- window 이후 같은 alert는 새 record로 저장된다.
- operational log pruning은 오래된 alert만 삭제하고 삭제 수와 cutoff를 반환한다.
- JDBC repository는 duplicate lookup과 alert pruning을 지원한다.

## 후속 작업

- Slack/Webhook adapter와 contract test를 추가한다.
- pruning 실행 이력 저장과 조회 API를 추가한다.
- 다중 인스턴스 환경의 DB 원자 suppression 정책을 결정한다.
- 실제 identity/role scope와 운영 alert 조치 권한을 연결한다.
