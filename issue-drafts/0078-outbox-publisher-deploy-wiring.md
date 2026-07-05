# 배포 outbox publisher 무동작(event 유실)과 endpoint 경로 정합

## 증상

배포(`postgres`) 프로파일에서 outbox 발행이 무동작한다.

- `deploy/k8s/app.yaml`이 `AI_REPO_OUTBOX_PUBLISHER_TYPE`를 주입하지 않아 `application.yml` 기본값 `memory`(`InMemoryOperationOutboxPublisher`, `matchIfMissing=true`)가 선택된다. relay는 event를 `PUBLISHED`로 마킹하지만 in-memory 큐에만 쌓여 컨슈머로 전달되지 않고, 재시작 시 유실된다.
- `http`로 바꿔도 기본 endpoint(`http://127.0.0.1:18080/outbox-events`)가 컨슈머 매핑(`/internal/broker/outbox-events`)과 불일치해 발행이 실패한다.

## 결정

- `deploy/k8s/app.yaml`에 `AI_REPO_OUTBOX_PUBLISHER_TYPE=http`와 `AI_REPO_OUTBOX_PUBLISHER_HTTP_ENDPOINT=http://ai-repo:8080/internal/broker/outbox-events` 주입. 앱이 자기 Service endpoint로 발행해 publish→consume loop 완성.
- `application.yml`의 http 기본 endpoint 경로를 `/outbox-events` → `/internal/broker/outbox-events`로 정합(host/port 유지).
- broker-token은 이미 secret으로 주입되므로 그대로 둔다.

## 검증

```bash
AI_REPO_DEV_RULES_BASE=origin/main bash scripts/check-dev-rules.sh
```

## 후속 (main 머지 후)

- 배포 프로파일(`postgres`/`prod`)에서 publisher type이 `memory`이면 startup을 실패시키는 fail-fast 가드(`BrokerTokenGuard` 패턴).

관련: GitHub Issue #144, ADR-0067, progress 0078.
