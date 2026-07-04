# 로컬 k8s 배포 + Grafana 모니터링

Docker Desktop 내장 Kubernetes에 ai-repo 전체 스택(앱 + Postgres + Prometheus + Grafana)을 배포하는 가이드.

## 전제 조건

- Docker Desktop 실행 중, **Settings → Kubernetes → Enable Kubernetes** 활성화
- `kubectl` 설치 (`docker-desktop` context 자동 생성됨)

## 기동 / 정리

```bash
./scripts/k8s-local-up.sh    # 이미지 빌드 → kubectl apply -k → 롤아웃 대기
./scripts/k8s-local-down.sh  # 전체 제거 (namespace 삭제, Postgres 데이터 포함)
```

| 서비스 | URL | 비고 |
| --- | --- | --- |
| 앱 | http://localhost:30080 | postgres 프로파일, outbox relay scheduler 활성 |
| Prometheus | http://localhost:30990 | `/targets`에서 ai-repo job UP 확인 |
| Grafana | http://localhost:30300 | 익명 Admin(로컬 전용), `ai-repo Overview` 대시보드 자동 프로비저닝 |

## 구성

```
deploy/k8s/
├── kustomization.yaml
├── namespace.yaml          # ai-repo 네임스페이스
├── postgres.yaml           # postgres:17-alpine + PVC(1Gi), compose.yml과 동일 자격증명
├── app.yaml                # ai-repo:local 이미지(imagePullPolicy: Never), NodePort 30080
├── prometheus.yaml         # /actuator/prometheus 15s 스크랩, 3d 보존, NodePort 30990
├── grafana.yaml            # 데이터소스·대시보드 프로비저닝, NodePort 30300
└── grafana-dashboard.yaml  # ai-repo Overview 대시보드 JSON
```

- **메트릭 노출**: `micrometer-registry-prometheus` + `management.endpoints.web.exposure.include: health,prometheus`. HTTP 지연시간 분위수(p95/p99)를 위해 `percentiles-histogram.http.server.requests: true`.
- **대시보드 패널**: HTTP 요청률/오류율/p95·p99, 지갑 거래 요청률(charge·transfer·payment POST), JVM Heap/GC, Hikari 커넥션, CPU, up.
- **이미지 태그**: 로컬은 `ai-repo:local`. CI/CD 도입 시 `{version}-{sha8}` 태그로 전환하고 GitOps 매니페스트를 갱신한다(ArgoCD sync — 후속 작업).
- **보안 주의**: 익명 Grafana Admin, 평문 DB 자격증명은 로컬 학습 환경 전용이다. 원격 클러스터에 그대로 쓰지 않는다.

## 스모크 확인

```bash
curl -s http://localhost:30080/actuator/health | jq .status          # "UP"
curl -s http://localhost:30080/actuator/prometheus | head            # 메트릭 텍스트
curl -s http://localhost:30990/api/v1/targets | jq '.data.activeTargets[].health'  # "up"
```

트래픽을 만들어 대시보드를 확인하려면 지갑 API를 호출한다(예: `scripts/mvp-local-smoke.sh` 참고).

## 후속 작업 (로드맵)

- GitHub Actions 이미지 빌드 → `{version}-{sha8}` 푸시 → GitOps 매니페스트 갱신 → ArgoCD sync
- outbox relay/consumer 커스텀 Micrometer 지표 노출(현재는 HTTP/JVM/Hikari 표준 지표 기반)
