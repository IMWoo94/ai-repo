#!/usr/bin/env bash
# 로컬 k8s의 ai-repo 스택을 제거한다. (namespace 삭제로 Postgres 데이터도 함께 초기화됨)
set -euo pipefail
cd "$(dirname "$0")/.."

kubectl --context docker-desktop delete -k deploy/k8s --ignore-not-found
echo "정리 완료 (Postgres 데이터 포함 전체 삭제)"
