#!/usr/bin/env bash
# 로컬 k8s의 ai-repo 스택을 제거한다. (namespace 삭제로 Postgres 데이터도 함께 초기화됨)
set -euo pipefail
cd "$(dirname "$0")/.."

# opt-in ELK 로깅 스택도 함께 내린다 (AI_REPO_ELK_ENABLED=true 일 때).
if [[ "${AI_REPO_ELK_ENABLED:-false}" == "true" ]]; then
  "$(dirname "$0")/elk-local-down.sh" || true
fi

kubectl --context docker-desktop delete -k deploy/k8s --ignore-not-found
echo "정리 완료 (Postgres 데이터 포함 전체 삭제)"
