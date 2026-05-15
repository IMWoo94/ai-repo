# 0059. Consumer Duplicate Spike Alert

## 스펙 목표

Consumer duplicate delivery가 운영상 이상 징후인지 판단할 수 있도록 health summary와 threshold 기반 alert 판정을 추가한다.

## 완료 결과

- `OperationOutboxConsumerHealthPolicy`, `Status`, `Summary`를 추가했다.
- `GET /api/v1/outbox-consumer/health` API를 추가했다.
- duplicate count와 duplicate rate 기준으로 `OK`, `WARNING`, `CRITICAL`, `NO_DATA`를 판정한다.
- 0060 단계에서 최근 window 기반 time bucket metric으로 health 판정 기준을 보강했다.
- 기본 threshold는 duplicate 5건 이상, warning 20%, critical 50%다.
- threshold는 `ai-repo.outbox-consumer.health.*` 설정으로 조정할 수 있다.

## 검증

- `./gradlew test --tests "*OperationOutboxConsumerMonitoringServiceTest" --tests "*OperationOutboxConsumerMonitoringControllerTest" --tests "*OperationOutboxConsumerServiceTest"`

## 남은 일

- delivery metric bucket pruning
- external alert channel
- broker-specific duplicate baseline

## 관련 문서

- `docs/adr/0049-consumer-duplicate-spike-alert.md`
- `docs/releases/unreleased.md`
- `wiki-drafts/QA-Scenarios.md`
