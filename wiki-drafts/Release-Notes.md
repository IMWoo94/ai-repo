# Release Notes

이 문서는 GitHub Wiki에 게시할 릴리스 요약 초안이다.

상세한 release note source는 `docs/releases/v0.7.0.md`이고, 이후 변경 후보는 `docs/releases/unreleased.md`에서 추적한다.

## 현재 후보: v0.7.0

`v0.7.0`은 `v0.6.0` 이후 `main`에 누적된 1차 MVP 출시 후보 변경분을 확정한 기준선이다.

## 포함된 큰 흐름

- React 사용자 화면 MVP
- Frontend unit/build/E2E gate
- 운영자 manual review console UI
- 운영자 relay health/pruning console UI
- Manual review requeue success E2E fixture
- Operator/admin token split
- Requeue approval workflow
- Requeue rejection workflow
- Direct requeue API deprecation
- Requeue state transition atomicity
- Outbox claim guarded result update
- Dev rules automatic sync check
- 운영자 console E2E smoke
- 운영 API 인증/인가와 Spring Security role model
- Outbox relay scheduler, run monitoring, health summary
- Admin API access audit
- Operational log pruning
- HTTP outbox broker adapter와 contract test
- HTTP outbox consumer adapter와 publish→consume loop
- Consumer monitoring admin API
- Consumer processed-event pruning
- Consumer duplicate spike alert
- Consumer duplicate time bucket metric
- Consumer delivery metric pruning
- Operational alert record channel
- Operational alert suppression and pruning
- Slack webhook operational alert publisher
- PostgreSQL scenario Testcontainers CI gate
- MVP local smoke script
- Wiki draft 최신화와 sync workflow
- 로컬 k8s 배포 스택(Prometheus/Grafana/Loki)과 outbox 커스텀 지표
- GitHub Actions → GHCR({version}-{sha8}) → GitOps → ArgoCD 배포 파이프라인
- Deploy 파이프라인 CI 성공 게이트(workflow_run)
- k8s 로컬 스크립트 하드닝(loki/alloy rollout 대기 + Loki ready 스모크, ArgoCD 설치 버전 고정 v3.4.4)
- JDBC persistence adapter 분해와 ArchUnit 레이어 규칙
- Broker consumer endpoint 인증 — `/internal/broker/outbox-events`에 `X-Broker-Token` shared secret(전용 Order 3 체인, 상수시간 비교, 미인증 401), publisher 동반 부착, 배포 프로파일 기본 토큰 `BrokerTokenGuard` fail-fast (ADR-0065)
- Grafana `ai-repo Overview` 대시보드에 Loki 로그 섹션 추가 — 로그 볼륨(level별 시계열)·앱 로그 스트림·오류/경고 로그 패널로 메트릭+로그를 한 화면에서 관측
- 배포 broker 토큰 주입 — `deploy/k8s` secret/env에 broker 토큰이 없어 `BrokerTokenGuard`가 배포 앱을 CrashLoopBackOff시키던 회귀 수정
- 송금 멱등 조회 순서 수정 — `transfer`가 멱등 조회를 잔액 검사보다 먼저 수행하도록 변경, 전액 송금 후 동일 키 재시도가 잔액 부족(422)으로 거부되던 회귀 수정, conflict(409) 감지 유지 (ADR-0069, #146)

## 릴리스 후보 검증

릴리스 PR 또는 tag 전에는 다음 명령과 CI job이 통과해야 한다.

- `./gradlew check`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `./gradlew test --tests '*LayerDependencyTest'`
- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `scripts/mvp-local-smoke.sh`
- `git diff --check`

## 알려진 제약

- Slack webhook 발행 실패 record와 재시도 정책은 아직 없다.
- 운영 API의 operator/admin identity는 local header 기반이다.
- 엔드유저 JWT는 발급/검증만 있고, 만료 후 갱신(refresh) 정책과 프론트엔드 로그인 연동은 후속이다.
- Kafka/RabbitMQ/SQS 같은 broker-specific adapter는 아직 없다.
- GitHub Wiki actual publication은 `v0.7.0` 기준으로 완료했다.

## 다음 후보

- opt-in ELK 로깅 스택(Filebeat→Logstash grok→Elasticsearch→Kibana) — `logging` 네임스페이스에 PLG와 병존, 기본 ArgoCD 자동배포에는 미포함(항상 켜짐 방지), 로그 파싱·역색인 검색 학습용. on/off는 독립 스크립트·`AI_REPO_ELK_ENABLED` env·별도 ArgoCD 수동 sync App(`deploy/argocd/logging-application.yaml`) 3가지 중 택1 (ADR-0066)
- JWT refresh token 기반 만료/갱신 정책 강화(현재는 401 시 password-less 재발급)
- 로그인 회원의 실제 보유 지갑 조회 API(현재는 fixture 기반 파생)
- broker-specific Testcontainers contract
- broker replay window별 retention 권장값
- pruning 실행 이력 저장과 조회 API
- Slack 발행 실패 record와 재시도 정책
- 운영 alert 화면 연결
- context별 JDBC adapter slice test 분리
