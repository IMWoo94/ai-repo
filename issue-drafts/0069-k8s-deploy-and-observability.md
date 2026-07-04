# 로컬 k8s 배포와 관측 스택 (B4 로드맵)

## 배경

ai-repo는 `compose.yml`(Postgres 단독)과 로컬 프로세스 실행에 의존했고, 배포·모니터링·로그 검색 체계가 없었다. 로컬에서 k8s로 전체 스택을 띄우고 Grafana 기반으로 지표·로그를 관측하며, main 머지 시 자동 배포되는 파이프라인이 필요하다.

## 목표

- Docker Desktop k8s에 앱 + Postgres + Prometheus + Grafana + Loki 전체 스택을 배포한다.
- Grafana 대시보드(ai-repo Overview)와 Explore 로그 검색(LogQL)을 제공한다.
- outbox relay/consumer 커스텀 Micrometer 지표를 노출한다.
- GitHub Actions → GHCR(`{version}-{sha8}`) → GitOps 매니페스트 갱신 → ArgoCD 자동 sync 파이프라인을 구성한다.
- 각 작업의 가이드(구동 방법, 접속 URL·계정, 로그 검색, 이슈 트래킹) 문서를 필수로 남긴다.

## 완료 조건

- [x] `deploy/k8s` kustomize 스택과 `scripts/k8s-local-up.sh`/`k8s-local-down.sh`로 기동·제거된다.
- [x] `/actuator/prometheus` 노출과 HTTP p95/p99 히스토그램이 활성화된다.
- [x] outbox 지표 6종(`ai_repo_outbox_*`)이 노출되고 대시보드 패널로 표시된다.
- [x] Loki + Alloy로 파드 로그가 수집되고 Grafana Explore에서 검색된다.
- [x] `deploy.yml`이 main push에서 이미지 push와 `deploy/gitops` newTag 갱신을 수행한다(`[skip ci]` 루프 차단).
- [x] ArgoCD Application이 `deploy/gitops`를 자동 sync(prune+selfHeal)하고 Synced/Healthy가 된다.
- [x] README 진입점 + 접속 정보 표 + 전체 명령어 모음 + 아키텍처 다이어그램(색상 구분 mermaid)이 문서화된다.
- [x] ADR-0059, progress 0069가 기록된다.

## 관련 문서

- `docs/adr/0059-k8s-deploy-and-observability.md`
- `docs/progress/0069-k8s-deploy-and-observability.md`
- `docs/development/k8s-local-monitoring.md`
- `docs/development/ci-cd-gitops.md`
- `docs/development/architecture-diagrams.md`
