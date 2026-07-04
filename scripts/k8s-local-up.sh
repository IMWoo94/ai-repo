#!/usr/bin/env bash
# 로컬 Docker Desktop k8s에 ai-repo + Postgres + Prometheus + Grafana를 배포한다.
set -euo pipefail
cd "$(dirname "$0")/.."

CONTEXT=docker-desktop

echo "==> Docker/K8s 확인"
docker info >/dev/null || { echo "Docker Desktop이 실행 중이어야 합니다."; exit 1; }
kubectl --context "$CONTEXT" get nodes >/dev/null || {
  echo "docker-desktop k8s 클러스터에 연결할 수 없습니다 (Docker Desktop 설정에서 Kubernetes 활성화)."; exit 1; }

echo "==> 이미지 빌드 (ai-repo:local)"
docker build -t ai-repo:local .

echo "==> 매니페스트 적용"
kubectl --context "$CONTEXT" apply -k deploy/k8s

echo "==> 롤아웃 대기"
kubectl --context "$CONTEXT" -n ai-repo rollout status deployment/postgres --timeout=180s
kubectl --context "$CONTEXT" -n ai-repo rollout status deployment/ai-repo --timeout=300s
kubectl --context "$CONTEXT" -n ai-repo rollout status deployment/prometheus --timeout=180s
kubectl --context "$CONTEXT" -n ai-repo rollout status deployment/loki --timeout=180s
kubectl --context "$CONTEXT" -n ai-repo rollout status deployment/alloy --timeout=180s
kubectl --context "$CONTEXT" -n ai-repo rollout status deployment/grafana --timeout=180s

echo "==> Loki 스모크 (ready)"
kubectl --context "$CONTEXT" -n ai-repo port-forward svc/loki 3100:3100 >/dev/null 2>&1 &
PF_PID=$!
trap 'kill "$PF_PID" 2>/dev/null || true' EXIT
for i in $(seq 1 10); do
  curl -sf http://localhost:3100/ready >/dev/null 2>&1 && break
  [[ $i -eq 10 ]] && { echo "Loki /ready 응답 없음 (port-forward svc/loki 3100)."; exit 1; }
  sleep 2
done
echo "Loki ready OK"
kill "$PF_PID" 2>/dev/null || true
trap - EXIT

cat <<'EOF'

배포 완료:
  앱          http://localhost:30080          (health: /actuator/health, metrics: /actuator/prometheus)
  Prometheus  http://localhost:30990          (targets: /targets)
  Grafana     http://localhost:30300          (익명 Admin, 대시보드: ai-repo Overview)
  Loki        Grafana Explore(Loki)에서 LogQL — Alloy가 파드 로그 수집

  (opt-in) ELK  AI_REPO_ELK_ENABLED=true 로 함께 기동 — Kibana http://localhost:30561
EOF

# opt-in ELK 로깅 스택 — AI_REPO_ELK_ENABLED=true 일 때만 함께 기동한다 (기본 off).
# 독립 제어는 scripts/elk-local-up.sh / elk-local-down.sh 로도 가능하다.
if [[ "${AI_REPO_ELK_ENABLED:-false}" == "true" ]]; then
  echo ""
  echo "==> (opt-in) ELK 로깅 스택 기동 — AI_REPO_ELK_ENABLED=true"
  "$(dirname "$0")/elk-local-up.sh"
fi
