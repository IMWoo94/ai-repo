# 0064. Slack Webhook Operational Alert Publisher

## 스펙 목표

Operational alert record를 Slack Incoming Webhook 호환 push channel로도 전송할 수 있게 한다.

## 완료 결과

- `OperationalAlertPublisher` port를 추가했다.
- 기본 no-op publisher와 선택형 Slack webhook publisher를 추가했다.
- Slack publisher는 `POST application/json`으로 text payload를 전송한다.
- Slack publisher bean 선택과 webhook HTTP contract test를 추가했다.
- Slack 발행 실패가 operational alert record 저장과 health API 응답을 깨지 않도록 했다.

## 검증

- `./gradlew test --tests "*OperationalAlertServiceTest" --tests "*SlackWebhookOperationalAlertPublisherContractTest" --tests "*OperationalAlertPublisherConfigurationTest"`

## 남은 일

- Slack 발행 실패 record와 재시도 정책
- production secret 주입과 channel routing 정책
- 운영 alert 화면 연결

## 관련 문서

- `docs/adr/0054-slack-webhook-operational-alert-publisher.md`
- `docs/releases/unreleased.md`
- `wiki-drafts/QA-Scenarios.md`
