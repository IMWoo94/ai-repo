#!/usr/bin/env bash
# 로컬 Docker Desktop k8s에 ArgoCD를 설치하고 ai-repo Application을 등록한다.
set -euo pipefail
cd "$(dirname "$0")/.."

CONTEXT=docker-desktop
# 재현성: 설치 매니페스트는 고정 버전을 사용한다(stable 태그는 이동하므로 금지).
ARGOCD_VERSION=v3.4.4

echo "==> ArgoCD 설치 (${ARGOCD_VERSION})"
kubectl --context "$CONTEXT" create namespace argocd --dry-run=client -o yaml | kubectl --context "$CONTEXT" apply -f -
# --server-side: ArgoCD CRD가 client-side apply 주석 크기 제한(256KiB)을 초과하므로 필수
kubectl --context "$CONTEXT" apply -n argocd --server-side --force-conflicts -f "https://raw.githubusercontent.com/argoproj/argo-cd/${ARGOCD_VERSION}/manifests/install.yaml"

echo "==> ArgoCD 서버 대기"
kubectl --context "$CONTEXT" -n argocd rollout status deployment/argocd-server --timeout=300s
kubectl --context "$CONTEXT" -n argocd rollout status deployment/argocd-repo-server --timeout=300s

echo "==> ai-repo Application 등록"
kubectl --context "$CONTEXT" apply -f deploy/argocd/application.yaml

PASSWORD=$(kubectl --context "$CONTEXT" -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d)

cat <<EOF

ArgoCD 설치 완료:
  UI 접속:   kubectl -n argocd port-forward svc/argocd-server 8443:443
             https://localhost:8443 (admin / ${PASSWORD})
  상태 확인: kubectl -n argocd get application ai-repo
EOF
