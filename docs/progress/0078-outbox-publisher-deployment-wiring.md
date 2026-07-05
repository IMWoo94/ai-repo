# 0078. Outbox Publisher Deployment Wiring

## 스펙 목표

- Issue #144의 배포(`postgres`) 프로파일 outbox 발행 무동작(event 유실)을 닫는다.
- 배포 relay가 자기 서비스의 컨슈머 endpoint로 실제 HTTP 발행을 하도록 배선한다.
- http 모드 기본 endpoint 경로를 컨슈머 매핑과 정합시킨다.

## 완료 결과

- `deploy/k8s/app.yaml` 컨테이너 env에 두 항목을 추가했다.
  - `AI_REPO_OUTBOX_PUBLISHER_TYPE=http` — memory 기본값 대신 HTTP publisher 선택.
  - `AI_REPO_OUTBOX_PUBLISHER_HTTP_ENDPOINT=http://ai-repo:8080/internal/broker/outbox-events` — 앱이 자기 Service endpoint로 발행해 publish→consume loop 완성.
- `src/main/resources/application.yml`의 http 기본 endpoint 경로를 `/outbox-events` → `/internal/broker/outbox-events`로 변경(host/port `127.0.0.1:18080` 유지)해 컨슈머 매핑과 일치시켰다.
- broker-token(`AI_REPO_OUTBOX_PUBLISHER_HTTP_BROKER_TOKEN`)은 이미 secret으로 주입되므로 그대로 두었다.
- ADR-0067로 두 결함(memory 기본·경로 불일치)과 결정을 기록하고, unreleased release note에 반영했다.

## 검증

- `AI_REPO_DEV_RULES_BASE=origin/main bash scripts/check-dev-rules.sh` = PASS
- java/migration/frontend 미변경이라 테스트 강제 대상 아님(배포 매니페스트·설정 기본값 변경).

## 남은 일

- 배포 프로파일(`postgres`/`prod`)에서 publisher type이 `memory`이면 startup을 실패시키는 fail-fast 가드(`BrokerTokenGuard` 패턴)는 후속으로 둔다. 현재는 env 누락 시 다시 memory로 조용히 무동작한다.

## 관련 문서

- `docs/adr/0067-outbox-publisher-deployment-wiring.md`
- `docs/releases/unreleased.md`
- `issue-drafts/0078-outbox-publisher-deploy-wiring.md`
