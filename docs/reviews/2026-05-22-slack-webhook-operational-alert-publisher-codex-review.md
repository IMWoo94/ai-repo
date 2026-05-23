# 2026-05-22 Slack Webhook Operational Alert Publisher Codex Review

## 범위

- `OperationalAlertPublisher` port
- 기본 no-op publisher
- Slack Incoming Webhook 호환 publisher
- alert service의 외부 발행 호출과 실패 격리
- Slack publisher configuration/contract tests
- README, ADR, progress, release, wiki draft 동기화

## 수용

1. Slack text의 다중 reason 줄바꿈 표현
   - 자체 리뷰에서 reason join 문자열이 Slack text 안에서 실제 줄바꿈 의도를 충분히 검증하지 못하는 점을 확인했다.
   - `SlackWebhookOperationalAlertPublisher`에서 실제 newline으로 reason을 조합하고 JSON escape 후 `\\n-` 형태로 전송되는지 contract test를 보강했다.

2. 실제 Slack webhook live test
   - 기본 CI 테스트가 외부 Slack에 의존하지 않도록 `AI_REPO_LIVE_SLACK_TEST=true`일 때만 실행되는 live integration test를 추가했다.
   - `--rerun-tasks`를 함께 사용해야 Gradle up-to-date 처리 없이 실제 요청이 전송된다.

## 반박

없음.

## 후속 과제

- Slack 발행 실패 record와 재시도 정책
- production secret 주입과 channel routing 정책
- 운영 alert 화면에서 Slack 발행 상태를 표시할지 검토

## 블로커

없음.

## 최종 판단

통과. Slack webhook adapter는 기본 no-op 설정을 유지하고, 설정 시 fake endpoint contract test로 payload와 실패 처리를 검증한다. 외부 발행 실패는 local alert record 저장과 health API 응답을 깨지 않는다.
