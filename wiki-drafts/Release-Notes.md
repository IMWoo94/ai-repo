# Release Notes

이 문서는 GitHub Wiki에 게시할 릴리스 요약 초안이다.

상세한 release note source는 `docs/releases/v0.7.0.md`이고, 이후 변경 후보는 `docs/releases/unreleased.md`에서 추적한다.

## 현재 후보: v0.7.0

`v0.7.0`은 `v0.6.0` 이후 `main`에 누적된 1차 MVP 출시 후보 변경분을 확정한 기준선이다.

## 포함된 큰 흐름

- React 사용자 화면 MVP
- Frontend unit/build/E2E gate
- 운영자 manual review console UI
- 운영자 console E2E smoke
- 운영 API 인증/인가와 Spring Security role model
- Outbox relay scheduler, run monitoring, health summary
- Admin API access audit
- Operational log pruning
- HTTP outbox broker adapter와 contract test
- PostgreSQL scenario Testcontainers CI gate
- MVP local smoke script
- Wiki draft 최신화와 sync workflow

## 릴리스 후보 검증

릴리스 PR 또는 tag 전에는 다음 명령과 CI job이 통과해야 한다.

- `./gradlew check`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `scripts/mvp-local-smoke.sh`
- `git diff --check`

## 알려진 제약

- 운영자 requeue 성공 E2E는 manual review fixture 정책이 필요하다.
- 운영자 relay health/pruning 화면은 아직 없다.
- 운영자 승인 워크플로우와 external alert channel은 아직 없다.
- admin token과 operator identity는 local header 기반이다.
- Kafka/RabbitMQ/SQS 같은 broker-specific adapter는 아직 없다.
- GitHub Wiki actual publication은 release operation으로 수행한다.

## 다음 후보

- manual review requeue full E2E fixture
- relay health/pruning operator UI
- broker-specific Testcontainers contract
- consumer idempotency
- external alert channel
