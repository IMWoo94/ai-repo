# 0069. 로컬 k8s 배포와 관측 스택 (B4 로드맵)

## 스펙 목표

- Docker Desktop k8s에 ai-repo 전체 스택을 로컬 배포하고 Grafana 기반 모니터링을 구축한다.
- Grafana에서 로그 검색이 가능해야 한다(Loki).
- main 머지 시 자동 배포되는 CI/CD GitOps 파이프라인(GitHub Actions → GHCR → ArgoCD)을 구성한다.
- outbox relay/consumer 커스텀 Micrometer 지표를 노출한다.

## 완료 결과

- **k8s 스택**(`deploy/k8s`, kustomize): 앱(postgres 프로파일, liveness/readiness probe, NodePort 30080) + Postgres 17(PVC) + Prometheus(15s 스크랩, NodePort 30990) + Grafana(데이터소스·대시보드 프로비저닝, NodePort 30300) + Loki(72h 보존) + Alloy(k8s API 로그 수집). `scripts/k8s-local-up.sh`/`k8s-local-down.sh`.
- **메트릭**: micrometer-registry-prometheus + `/actuator/prometheus` 노출, HTTP p95/p99 히스토그램. 커스텀 outbox 지표 6종(`ai_repo_outbox_relay_runs_total`, `relay_published_events_total`, `relay_pending_events`, `relay_health_status`, `consumer_events_total`, `consumer_health_status`) — 비침습 브리지(`OutboxMetricsBinder` 게이지 + `OutboxRelayMetricsRecorder` 포트 카운터).
- **대시보드**: `ai-repo Overview` 16패널 — HTTP 요청률/오류율/p95·p99, 지갑 거래율, JVM/GC, Hikari, CPU, up + Outbox 릴레이/컨슈머 row.
- **로그 검색**: Grafana Explore에서 LogQL(`{app="ai-repo"} |= "ERROR"` 등), 라벨 namespace/pod/container/app.
- **CI/CD**: `.github/workflows/deploy.yml` — CI("CI" 워크플로우) 성공 시에만 `workflow_run`으로 트리거(`if: conclusion == 'success'`). CI run의 `head_sha`를 체크아웃해 `{version}-{sha8}` 태그로 ghcr.io/imwoo94/ai-repo push 후 `deploy/gitops` newTag 갱신(`[skip ci]`). ArgoCD Application(`deploy/argocd`)이 자동 sync(prune+selfHeal). 설치는 `scripts/argocd-install.sh`(--server-side).
- **문서**: `docs/development/k8s-local-monitoring.md`(가이드·로그 검색·이슈 트래킹), `docs/development/ci-cd-gitops.md`(파이프라인·트러블슈팅·모드 전환·이슈 트래킹), ADR-0059.

## 개선 건수

1. 로컬 k8s 배포 + Prometheus/Grafana/Loki 관측 스택 신설.
2. outbox relay/consumer 커스텀 지표 6종 노출과 대시보드 반영.
3. GitHub Actions → GHCR → GitOps → ArgoCD 자동 배포 파이프라인 신설.

## 검증

- `./gradlew test` 통과(신규 지표 브리지 단위 테스트 포함), `./gradlew postgresScenarioTest` 통과
- 실배포 스모크: health UP → 충전 API 트래픽 → Prometheus target up·지표 수집 → Grafana 대시보드/Loki LogQL 조회 확인
- ArgoCD Application Synced/Healthy (검증은 feature 브랜치 대상, main 머지 후 `deploy/argocd/application.yaml` 재적용)

## 남은 일

- main 머지 후 첫 deploy.yml 실행으로 실제 ghcr push·GitOps 갱신 검증(그 전까지 ghcr 패키지 미존재)
- ghcr 패키지 public 전환(로컬 노드 익명 pull용) 또는 pull secret 등록
- ~~배포 전 CI 게이트(`workflow_run`) 도입 여부 검토~~ → 완료: deploy를 CI 성공(`workflow_run` + `if: conclusion == 'success'`)에 게이트해 깨진 커밋 배포를 차단.

## 관련 문서

- GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/110
- `docs/adr/0059-k8s-deploy-and-observability.md`
- `docs/development/k8s-local-monitoring.md`
- `docs/development/ci-cd-gitops.md`
