#!/usr/bin/env bash
# 로컬 k8s의 opt-in ELK 로깅 스택을 제거한다. (namespace 삭제로 ES 데이터(emptyDir)도 함께 초기화됨)
set -euo pipefail
cd "$(dirname "$0")/.."

CONTEXT=${CONTEXT:-docker-desktop}

kubectl --context "$CONTEXT" delete -k deploy/k8s/logging --ignore-not-found
echo "정리 완료 (ELK 스택 전체 삭제, ES 데이터 포함)"
