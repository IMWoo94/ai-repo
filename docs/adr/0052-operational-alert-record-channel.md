# ADR-0052: Operational Alert Record Channel

## 상태

Accepted

## 배경

Relay health와 consumer duplicate health는 `WARNING`, `CRITICAL` 상태와 `alertReasons`를 반환한다. 하지만 운영자가 health endpoint를 직접 열어보지 않으면 alert 이력이 남지 않는다.

Slack/Webhook 같은 외부 push channel을 바로 붙이기 전, 로컬·포트폴리오 환경에서는 alert를 운영 record로 남기고 조회할 수 있는 최소 channel이 필요하다.

## 결정

Health summary가 `WARNING` 또는 `CRITICAL`이면 `operational_alerts`에 alert record를 저장한다.

| 항목 | 결정 |
| --- | --- |
| 저장소 | `operational_alerts` |
| 조회 API | `GET /api/v1/operational-alerts?limit=50` |
| 권한 | 조회성 운영 API이므로 `ROLE_OPERATOR` 이상 |
| source | `OUTBOX_RELAY`, `OUTBOX_CONSUMER` |
| severity | `WARNING`, `CRITICAL` |
| reasons | health summary의 `alertReasons` |
| 발행 시점 | relay/consumer health summary 평가 시 |
| 제외 | `OK`, `NO_DATA`는 alert record를 남기지 않는다 |

## 트레이드오프

### 장점

- 로컬 환경에서도 alert 발생 이력을 API로 확인할 수 있다.
- Slack/Webhook 도입 전 alert publisher port의 최소 계약을 고정한다.
- alert reason을 그대로 저장해 운영자가 health 판정 근거를 추적할 수 있다.

### 비용

- 현재는 health 조회가 alert 발행 트리거다. 주기적 push alert는 후속 scheduler/webhook 작업이 필요하다.
- 동일 warning/critical 상태를 반복 조회하면 alert record가 반복 저장될 수 있다.
- alert record pruning 정책은 아직 없다.

## 검증 기준

- consumer health가 `CRITICAL`이면 `OUTBOX_CONSUMER` alert가 저장된다.
- relay health가 `WARNING` 또는 `CRITICAL`이면 `OUTBOX_RELAY` alert가 저장된다.
- `OK`, `NO_DATA` health는 alert를 저장하지 않는다.
- `GET /api/v1/operational-alerts`는 operator token으로 조회할 수 있고 token이 없으면 거부한다.

## 후속 작업

- alert dedupe/suppression window를 추가한다.
- alert record pruning 정책을 추가한다.
- Slack/Webhook adapter와 contract test를 추가한다.
