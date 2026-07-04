# ADR-0059: 로컬 k8s 배포와 관측(모니터링·로그·GitOps) 스택

## 상태

Accepted

## 배경

ai-repo는 지금까지 `compose.yml`(Postgres 단독)과 로컬 프로세스 실행에 의존했고, 배포·모니터링·로그 검색 체계가 없었다. 학습 랩 로드맵 B4로 "로컬에서 k8s로 띄우고 Grafana 기반으로 관측"하는 환경과, main 머지 시 자동 배포되는 GitOps 파이프라인이 필요했다.

## 결정

| 항목 | 결정 |
| --- | --- |
| 클러스터 | Docker Desktop 내장 Kubernetes(단일 노드). 별도 kind/minikube 미도입 |
| 매니페스트 | `deploy/k8s` kustomize 단일 base — 앱, Postgres(PVC), Prometheus, Grafana, Loki, Alloy |
| 메트릭 | micrometer-registry-prometheus, `/actuator/prometheus`를 Prometheus가 15s 스크랩. HTTP 지연 분위수는 percentiles-histogram |
| 커스텀 지표 | outbox 릴레이/컨슈머를 비침습 브리지로 노출 — 게이지는 `OutboxMetricsBinder`가 기존 모니터링 서비스를 스크레이프 시점에 읽고, 릴레이 카운터는 `OutboxRelayMetricsRecorder` 포트로 기록 시점 증가(실행 저장소가 최근 샘플만 보관해 사후 누적 불가하므로) |
| 로그 | Loki(single binary, filesystem, 72h) + Grafana Alloy가 k8s API로 파드 로그 tail(호스트 마운트 없음). Grafana Explore에서 LogQL 검색 |
| 대시보드 | Grafana 프로비저닝(ConfigMap) — `ai-repo Overview`: HTTP/지갑거래/JVM/Hikari/CPU + Outbox row |
| CI/CD | GitHub Actions CI 성공 후 deploy가 `workflow_run`으로 트리거(`if: conclusion == 'success'`) → CI run의 `head_sha` 체크아웃 → 이미지 태그 `{version}-{sha8}` → ghcr.io push → `deploy/gitops/kustomization.yaml` newTag 갱신 커밋(`[skip ci]`) |
| GitOps | ArgoCD(로컬 설치, `--server-side` apply)가 `deploy/gitops`를 자동 sync(prune+selfHeal). 오버레이는 `../k8s` base + ghcr 이미지 교체 |
| 모드 분리 | 같은 네임스페이스를 관리하므로 로컬 수동 모드(`k8s-local-up.sh`)와 GitOps 모드는 택1 |

## 트레이드오프

### 장점

- 지표·로그·대시보드가 Grafana 한 곳에 모이고, main 머지만으로 배포가 완결된다.
- 스택 전체가 리포 안 선언형 매니페스트라 재현·초기화가 쉽다(`k8s-local-down.sh` → up).
- 커스텀 지표가 기존 도메인 모니터링 서비스를 재사용해 이중 상태가 없다.

### 비용

- 익명 Grafana Admin·평문 자격증명·단일 replica는 로컬 학습 전용 구성이다. 원격 전환 시 Secret/인증/HA 재설계가 필요하다.
- deploy는 CI(ci.yml) 성공 시에만 `workflow_run`으로 트리거되어 깨진 커밋은 배포되지 않는다. 대신 paths 필터가 없어 CI를 통과한 docs-only push도 이미지 빌드+배포를 트리거한다(로컬 학습 랩 규모에서는 무해).
- Alloy가 k8s API로 로그를 tail하므로 초대형 로그 볼륨에는 부적합하다(로컬 규모에서는 무해).

## 대안

- kind/minikube: Docker Desktop k8s가 이미 있어 추가 도구 불필요.
- ELK/EFK 로그 스택: Grafana 통합·경량성에서 Loki가 학습 랩에 적합.
- CI에서 kubectl apply 직접 배포: 클러스터 자격증명이 CI로 나가고 상태 추적이 없어 GitOps(pull 방식)를 채택.
- Helm 차트: kustomize가 이미 base/overlay 요구를 충족하고 의존성이 없다.
