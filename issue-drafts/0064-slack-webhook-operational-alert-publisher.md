# Slack Webhook Operational Alert Publisher

## 배경

Operational alert는 DB record와 API 조회로 남지만, 운영자가 dashboard를 직접 열지 않으면 warning/critical 상태를 늦게 볼 수 있다. Slack Incoming Webhook 호환 adapter를 붙여 push channel 계약을 먼저 고정한다.

## 목표

- Operational alert 외부 발행 port를 추가한다.
- 기본 실행은 no-op으로 유지한다.
- 설정 시 Slack webhook으로 alert text payload를 전송한다.
- Slack 실패가 DB alert 저장과 health API를 깨지 않게 한다.

## 완료 조건

- [x] `OperationalAlertPublisher` port가 존재한다.
- [x] 기본 no-op publisher가 존재한다.
- [x] `slack-webhook` 설정에서 Slack webhook publisher가 선택된다.
- [x] Slack webhook adapter contract test가 method/header/body를 검증한다.
- [x] Slack 실패가 alert record 저장 흐름을 실패시키지 않는다.
- [x] ADR, progress, release, wiki draft가 갱신된다.

## 검증 명령

```bash
./gradlew test --tests "*OperationalAlertServiceTest" --tests "*SlackWebhookOperationalAlertPublisherContractTest" --tests "*OperationalAlertPublisherConfigurationTest"
scripts/check-dev-rules.sh
```
