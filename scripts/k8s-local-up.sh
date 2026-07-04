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
kubectl --context "$CONTEXT" -n ai-repo rollout status deployment/grafana --timeout=180s

cat <<'EOF'

배포 완료:
  앱          http://localhost:30080          (health: /actuator/health, metrics: /actuator/prometheus)
  Prometheus  http://localhost:30990          (targets: /targets)
  Grafana     http://localhost:30300          (익명 Admin, 대시보드: ai-repo Overview)
EOF
