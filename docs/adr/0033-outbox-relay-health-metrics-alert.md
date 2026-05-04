# ADR-0033: Outbox Relay Health Metrics and Alert

## 상태

Accepted

## 맥락

Outbox relay scheduler 실행 이력은 `operation_outbox_relay_runs`에 저장되고 운영 API로 최근 실행 목록을 조회할 수 있다. 하지만 운영자가 목록을 직접 읽어 실패율, 마지막 성공 시각, 연속 실패 횟수를 판단해야 했다.

금융/핀테크 운영에서는 자동 worker의 상태를 빠르게 판정할 수 있어야 한다. 특히 돈 이동 후 외부 event 발행이 지연되거나 실패하면 정합성 확인, 재처리, 고객 영향 판단이 늦어진다. 현재 단계에서는 외부 모니터링 시스템보다 코드와 테스트로 alert 판정 기준을 먼저 고정하는 것이 중요하다.

## 선택지

### 선택지 A: 최근 실행 목록만 유지한다

장점:

- 구현 변경이 없다.
- 운영자가 원본 실행 이력을 직접 확인할 수 있다.

단점:

- 연속 실패, 실패율, 마지막 성공 시각을 매번 사람이 해석해야 한다.
- alert 기준이 코드와 문서에 남지 않는다.
- 테스트로 운영 판정 기준을 고정하기 어렵다.

### 선택지 B: relay run 이력 기반 health summary API를 추가한다

장점:

- 기존 실행 이력을 재사용하므로 별도 metric backend가 없어도 된다.
- `OK`, `WARNING`, `CRITICAL`, `NO_DATA` 판정을 테스트로 고정할 수 있다.
- 마지막 성공/실패 시각, 실패율, 연속 실패 횟수를 운영 API로 확인할 수 있다.

단점:

- Prometheus/Grafana 같은 표준 metric pipeline은 아직 없다.
- 외부 알림 발송은 별도 작업이 필요하다.
- pruning으로 오래된 실행 이력이 삭제되면 summary는 최근 window 기준으로만 판단한다.

### 선택지 C: Micrometer/Actuator/Prometheus를 바로 도입한다

장점:

- 운영 환경에 가까운 metric 수집과 dashboard 구성이 가능하다.
- alert manager 연동으로 실제 알림까지 확장하기 쉽다.

단점:

- 현재 로컬 포트폴리오 단계에서는 운영 인프라 범위가 크다.
- metric endpoint만으로는 alert 판정 이유를 API 응답으로 설명하기 어렵다.
- 실제 broker adapter가 없는 상태에서 외부 dashboard부터 붙이면 검증 범위가 과해진다.

## 결정

relay run 이력 기반 health summary API를 추가한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 조회 API | `GET /api/v1/outbox-relay-runs/health` |
| 권한 | `X-Admin-Token`, `X-Operator-Id` 필요 |
| 상태 | `OK`, `WARNING`, `CRITICAL`, `NO_DATA` |
| 기본 sample size | 최근 20회 |
| warning 연속 실패 | 2회 |
| critical 연속 실패 | 3회 |
| warning 실패율 | 50% 이상 |
| critical 마지막 성공 지연 | 15분 초과 |
| 외부 알림 | 후속 작업 |

## 결과

장점:

- 운영자가 relay 상태를 원본 목록이 아니라 summary로 빠르게 확인할 수 있다.
- alert 판정 이유가 `alertReasons`로 함께 반환된다.
- threshold가 설정과 테스트로 고정된다.

비용:

- 실제 metric backend와 dashboard는 아직 없다.
- Slack/Email/PagerDuty 알림 발송은 아직 없다.
- health summary는 저장된 relay run window 안에서만 판단한다.

후속 작업:

- Micrometer/Actuator 기반 metric endpoint를 추가한다.
- 실제 broker adapter 도입 후 broker publish 실패와 relay health를 함께 검증한다.
- 외부 알림 채널과 escalation policy를 별도 ADR로 결정한다.
