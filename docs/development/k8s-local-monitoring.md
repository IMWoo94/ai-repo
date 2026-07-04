# 로컬 k8s 배포 + Grafana 모니터링 + 로그 검색

Docker Desktop 내장 Kubernetes에 ai-repo 전체 스택(앱 + Postgres + Prometheus + Grafana + Loki)을 배포하는 가이드.

## 전제 조건

- Docker Desktop 실행 중, **Settings → Kubernetes → Enable Kubernetes** 활성화
- `kubectl` 설치 (`docker-desktop` context 자동 생성됨)

## 기동 / 정리

```bash
./scripts/k8s-local-up.sh    # 이미지 빌드 → kubectl apply -k → 롤아웃 대기
./scripts/k8s-local-down.sh  # 전체 제거 (namespace 삭제, Postgres 데이터 포함)
```

## 접속 정보 (URL · 계정)

| 서비스 | URL | 계정 / 인증 |
| --- | --- | --- |
| 앱 API | http://localhost:30080 | 사용자 API 인증 없음. 운영 API는 헤더 `X-Operator-Token: local-operator-token`(조회) / `X-Admin-Token: local-ops-token`(변경) + `X-Operator-Id: <이름>`. 토큰 고정값 출처는 `deploy/k8s/secrets.yaml`(Secret `ai-repo-credentials`) |
| 앱 health | http://localhost:30080/actuator/health | 없음 |
| 앱 메트릭 | http://localhost:30080/actuator/prometheus | 없음 |
| Prometheus | http://localhost:30990 | 없음 |
| Prometheus 타겟 상태 | http://localhost:30990/targets | 없음 — ai-repo job UP 확인 |
| Grafana | http://localhost:30300 | **익명 Admin, 로그인 불필요**(로컬 전용) |
| Grafana 대시보드 (ai-repo Overview) | http://localhost:30300/d/ai-repo-overview | 익명 Admin |
| Grafana Explore (Loki 로그 검색) | http://localhost:30300/explore | 익명 Admin |
| Loki API | 클러스터 내부 http://loki:3100 — 외부는 port-forward 후 http://localhost:3100 | 없음 |
| Postgres | 클러스터 내부 postgres:5432 — 외부는 port-forward 후 localhost:5432 | 사용자 `ai_repo` / 비밀번호 `ai_repo` / DB `ai_repo` — 고정값 출처는 `deploy/k8s/secrets.yaml`(Secret `ai-repo-credentials`) |
| ArgoCD UI (선택) | port-forward 후 https://localhost:8443 | `admin` / 초기 비밀번호는 아래 명령으로 조회 |

DB 자격증명·운영 토큰(`AI_REPO_OPS_OPERATOR_TOKEN`, `AI_REPO_OPS_ADMIN_TOKEN`)·JWT 서명 비밀(`AI_REPO_AUTH_JWT_SECRET`)은 평문 env가 아니라 Secret `ai-repo-credentials`(`deploy/k8s/secrets.yaml`)에서 `secretKeyRef`로 주입된다. 값을 바꾸려면 `secrets.yaml`을 수정한다(로컬 전용 고정값이며, 원격에서는 별도 주입). ArgoCD 설치/GitOps 모드는 `docs/development/ci-cd-gitops.md` 참고.

## 전체 명령어 모음 (리포 루트 기준, 복사-실행)

```bash
# ── 스택 기동 / 제거 ──────────────────────────────────────────────
./scripts/k8s-local-up.sh                 # 이미지 빌드 → 배포 → 롤아웃 대기
./scripts/k8s-local-down.sh               # 전체 제거 (Postgres 데이터 포함)
./scripts/argocd-install.sh               # (선택) ArgoCD 설치 + ai-repo Application 등록

# ── 앱 확인 ──────────────────────────────────────────────────────
curl -s http://localhost:30080/actuator/health
curl -s http://localhost:30080/actuator/prometheus | grep ai_repo_outbox

# 사용자 API 예시 — 충전
curl -s -X POST http://localhost:30080/api/v1/wallets/wallet-001/charges \
  -H "Content-Type: application/json" \
  -d '{"amount":1000,"currency":"KRW","idempotencyKey":"smoke-001","description":"스모크 충전"}'

# 운영 API 예시 — relay health (operator 토큰)
curl -s http://localhost:30080/api/v1/outbox-relay-runs/health \
  -H "X-Operator-Token: local-operator-token" \
  -H "X-Operator-Id: local-dev"

# ── Prometheus ───────────────────────────────────────────────────
open http://localhost:30990/targets                                  # 타겟 UP 확인 (macOS)
curl -s -G http://localhost:30990/api/v1/query \
  --data-urlencode 'query=ai_repo_outbox_relay_runs_total'           # 지표 직접 조회

# ── Grafana ──────────────────────────────────────────────────────
open http://localhost:30300/d/ai-repo-overview                       # 대시보드
open http://localhost:30300/explore                                  # Loki 로그 검색 (LogQL)

# ── Loki 직접 조회 (Grafana 없이) ────────────────────────────────
kubectl --context docker-desktop -n ai-repo port-forward svc/loki 3100:3100 &
curl -s -G http://localhost:3100/loki/api/v1/query_range \
  --data-urlencode 'query={app="ai-repo"} |= "ERROR"' \
  --data-urlencode 'limit=20'

# ── Postgres 직접 접속 ───────────────────────────────────────────
kubectl --context docker-desktop -n ai-repo port-forward svc/postgres 5432:5432 &
psql -h localhost -p 5432 -U ai_repo -d ai_repo                      # 비밀번호: ai_repo

# ── ArgoCD (선택, GitOps 모드) ───────────────────────────────────
kubectl --context docker-desktop -n argocd port-forward svc/argocd-server 8443:443 &
open https://localhost:8443                                          # admin / 아래 비밀번호
kubectl --context docker-desktop -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath='{.data.password}' | base64 -d; echo
kubectl --context docker-desktop -n argocd get application ai-repo   # Synced / Healthy 확인

# ── 파드 상태 / 즉석 로그 ────────────────────────────────────────
kubectl --context docker-desktop -n ai-repo get pods -o wide
kubectl --context docker-desktop -n ai-repo logs deploy/ai-repo --tail=100 -f
kubectl --context docker-desktop -n ai-repo get events --sort-by=.lastTimestamp
```

## 구성

```
deploy/k8s/
├── kustomization.yaml
├── namespace.yaml          # ai-repo 네임스페이스
├── secrets.yaml            # Secret ai-repo-credentials — DB·운영 토큰·JWT 로컬 고정값(원격은 별도 주입)
├── postgres.yaml           # postgres:17-alpine + PVC(1Gi), 자격증명은 secrets.yaml에서 secretKeyRef 주입
├── app.yaml                # ai-repo:local 이미지(imagePullPolicy: Never), NodePort 30080
├── prometheus.yaml         # /actuator/prometheus 15s 스크랩, 3d 보존, NodePort 30990
├── loki.yaml               # Loki 3.5 single binary, filesystem 저장, 72h 보존
├── alloy.yaml              # Grafana Alloy — ai-repo 네임스페이스 파드 로그를 k8s API로 수집해 Loki 전송
├── grafana.yaml            # 데이터소스(Prometheus·Loki)·대시보드 프로비저닝, NodePort 30300
└── grafana-dashboard.yaml  # ai-repo Overview 대시보드 JSON
```

- **메트릭 노출**: `micrometer-registry-prometheus` + `management.endpoints.web.exposure.include: health,prometheus`. HTTP 지연시간 분위수(p95/p99)를 위해 `percentiles-histogram.http.server.requests: true`.
- **대시보드 패널**: HTTP 요청률/오류율/p95·p99, 지갑 거래 요청률(charge·transfer·payment POST), JVM Heap/GC, Hikari 커넥션, CPU, up + Outbox 릴레이/컨슈머 row(아래 커스텀 지표).
- **이미지 태그**: 로컬은 `ai-repo:local`. GitOps 배포는 `ghcr.io/imwoo94/ai-repo:{version}-{sha8}` — `docs/development/ci-cd-gitops.md` 참고.
- **보안 주의**: 자격증명은 Secret `ai-repo-credentials`(`deploy/k8s/secrets.yaml`)로 분리했으나 로컬 학습 편의를 위해 평문으로 커밋한 고정값이다. 익명 Grafana Admin과 함께 로컬 학습 환경 전용이며, 원격 클러스터에서는 Secret을 별도 주입(External Secrets/SealedSecrets 등)한다.

## Outbox 커스텀 지표

기존 outbox 모니터링 도메인 서비스를 재구조화하지 않고 비침습적 브리지로 노출한다. 게이지(pending, health, consumer 누적)는 `OutboxMetricsBinder`(MeterBinder)가 스크레이프 시점에 모니터링 서비스를 읽고, 릴레이 실행/발행 카운터는 `OutboxRelayMetricsRecorder` 포트를 통해 `recordSuccess/recordFailure` 시점에 증가시켜 단조 증가를 보장한다(릴레이 실행 저장소는 최근 샘플만 보관하므로 사후 누적 불가). health enum은 0/1/2 수치로 매핑해 알람 임계값에 바로 쓸 수 있다.

| Prometheus 지표 | 타입 | 태그 |
| --- | --- | --- |
| `ai_repo_outbox_relay_runs_total` | counter | `result=success\|failure` |
| `ai_repo_outbox_relay_published_events_total` | counter | `outcome=published\|failed` |
| `ai_repo_outbox_relay_pending_events` | gauge | — |
| `ai_repo_outbox_relay_health_status` | gauge | 0=ok, 1=warning, 2=critical |
| `ai_repo_outbox_consumer_events_total` | counter | `outcome=processed\|duplicate` |
| `ai_repo_outbox_consumer_health_status` | gauge | 0=ok, 1=warning, 2=critical |

(컨슈머는 실패 시 예외로 중단되어 기록이 남지 않으므로 `failed` outcome이 없다 — 도메인 의미론과 일치.)

## 로그 검색 (Loki)

수집 경로: Alloy가 ai-repo 네임스페이스의 모든 파드 로그를 k8s API로 tail(호스트 마운트 없음) → Loki(72h 보존). 라벨: `namespace`, `pod`, `container`, `app`.

**Grafana Explore** (http://localhost:30300 → Explore → Loki) LogQL 예시:

```logql
{app="ai-repo"}                                  # 앱 전체 로그
{app="ai-repo"} |= "ERROR"                       # 에러만
{app="ai-repo"} |= "op-001"                      # 특정 operation 추적
{app="ai-repo"} |~ "Transfer|TRANSFER"           # 정규식 매칭
{namespace="ai-repo", app="postgres"}            # DB 로그
sum by (app) (rate({namespace="ai-repo"}[5m]))   # 컴포넌트별 로그 발생률
```

즉석 확인(kubectl):

```bash
kubectl -n ai-repo logs deploy/ai-repo --tail=100 -f          # 앱 로그 follow
kubectl -n ai-repo logs deploy/ai-repo --previous             # 재시작 직전 로그(크래시 원인)
kubectl -n ai-repo get events --sort-by=.lastTimestamp        # 파드 이벤트(OOM, probe 실패 등)
```

## 스모크 확인

```bash
curl -s http://localhost:30080/actuator/health | jq .status          # "UP"
curl -s http://localhost:30080/actuator/prometheus | grep ai_repo_outbox | head   # 커스텀 지표
curl -s http://localhost:30990/api/v1/targets | jq '.data.activeTargets[].health'  # "up"
kubectl -n ai-repo port-forward svc/loki 3100:3100 &                 # Loki 직접 조회 시
curl -s -G http://localhost:3100/loki/api/v1/query_range --data-urlencode 'query={app="ai-repo"}' --data-urlencode 'limit=5'
```

트래픽을 만들어 대시보드를 확인하려면 지갑 API를 호출한다(예: `scripts/mvp-local-smoke.sh` 참고).

## 이슈 트래킹

모니터링/로그에서 문제(지표 이상, 에러 로그, 파드 크래시)를 발견하면:

1. Grafana 패널 스크린샷 또는 LogQL 쿼리 결과, `kubectl describe` 출력을 증적으로 수집
2. `issue-drafts/`에 `.github/ISSUE_TEMPLATE/bug.yml` 형식으로 초안 작성(재현 절차 + 증적 포함)
3. GitHub Issue 생성 후 `issue-drafts/README.md` 목록에 링크 기록, 수정 PR에 이슈 연결

## 관련 문서

- `docs/development/ci-cd-gitops.md` — GitHub Actions → GHCR → GitOps → ArgoCD 배포 파이프라인
- `docs/adr/0059-k8s-deploy-and-observability.md` — 배포·관측 설계 결정
