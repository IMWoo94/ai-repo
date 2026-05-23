# ADR-0054: Slack Webhook Operational Alert Publisher

## 상태

Accepted

## 배경

Operational alert는 DB record와 조회 API로 남지만, 운영자는 dashboard를 계속 보고 있지 않는다. Slack 같은 외부 알림 채널을 붙이면 warning/critical health를 더 빠르게 인지할 수 있다.

다만 실제 Slack OAuth app이나 bot token을 바로 도입하면 인증 범위와 secret 관리가 커진다. 현재 단계에서는 Incoming Webhook 호환 HTTP contract로 push channel 경계를 먼저 고정한다.

## 결정

Operational alert publisher port와 Slack webhook adapter를 추가한다.

| 항목 | 결정 |
| --- | --- |
| port | `OperationalAlertPublisher` |
| 기본 adapter | `NoopOperationalAlertPublisher` |
| Slack adapter | `SlackWebhookOperationalAlertPublisher` |
| 활성화 설정 | `ai-repo.operational-alert.publisher.type=slack-webhook` |
| webhook URL | `ai-repo.operational-alert.publisher.slack.webhook-url` |
| timeout 기본값 | 3000ms |
| payload | Slack Incoming Webhook 호환 `{"text":"..."}` |
| 실패 정책 | DB alert 저장은 유지하고 외부 발행 실패는 health API 실패로 전파하지 않는다 |

## 트레이드오프

### 장점

- Slack token scope 없이 webhook URL만으로 alert push 경계를 검증할 수 있다.
- 기본 profile은 no-op이라 로컬 실행에 secret이 필요 없다.
- fake HTTP endpoint contract test로 method/header/body와 실패 처리를 검증한다.

### 비용

- Webhook 실패 자체를 별도 운영 record로 남기지 않는다.
- Slack app lifecycle, channel routing, mention policy는 아직 없다.
- 메시지는 단순 text payload라 rich block layout은 후속으로 남긴다.

## 검증 기준

- 기본 설정에서는 no-op publisher bean이 선택된다.
- `slack-webhook` 설정에서는 Slack webhook publisher bean이 선택된다.
- Slack adapter는 `POST application/json`으로 text payload를 전송한다.
- Slack webhook non-2xx 응답은 adapter 내부에서는 실패로 표현되지만 health alert 저장 흐름은 깨지지 않는다.

## 후속 작업

- Slack 발행 실패 record와 재시도 정책을 결정한다.
- 운영 alert 화면에서 Slack 발행 여부를 표시할지 검토한다.
- production secret 주입과 channel routing 정책을 정한다.
