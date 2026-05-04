#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${AI_REPO_BACKEND_URL:-http://127.0.0.1:8080}"
FRONTEND_URL="${AI_REPO_FRONTEND_URL:-http://127.0.0.1:5173}"
ADMIN_TOKEN="${AI_REPO_OPS_ADMIN_TOKEN:-local-ops-token}"
OPERATOR_ID="${AI_REPO_SMOKE_OPERATOR_ID:-local-smoke-operator}"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

check_contains() {
  local value="$1"
  local expected="$2"
  local label="$3"

  if [[ "$value" != *"$expected"* ]]; then
    fail "$label did not contain '$expected'"
  fi
}

echo "MVP local smoke"
echo "- backend:  ${BACKEND_URL}"
echo "- frontend: ${FRONTEND_URL}"

health_response="$(curl -fsS "${BACKEND_URL}/actuator/health")" \
  || fail "actuator health endpoint is not reachable"
check_contains "$health_response" '"status":"UP"' "actuator health response"

wallet_response="$(curl -fsS "${BACKEND_URL}/api/v1/wallets/wallet-001/balance")" \
  || fail "backend wallet balance API is not reachable"
check_contains "$wallet_response" '"walletId":"wallet-001"' "wallet balance response"

manual_review_response="$(
  curl -fsS \
    -H "X-Admin-Token: ${ADMIN_TOKEN}" \
    -H "X-Operator-Id: ${OPERATOR_ID}" \
    "${BACKEND_URL}/api/v1/outbox-events/manual-review?limit=20"
)" || fail "operator manual review API is not reachable with local admin headers"
check_contains "$manual_review_response" '[' "manual review response"

auth_error_response="$(
  curl -sS \
    -H "X-Admin-Token: wrong-token" \
    -H "X-Operator-Id: ${OPERATOR_ID}" \
    "${BACKEND_URL}/api/v1/outbox-events/manual-review?limit=20"
)" || true
check_contains "$auth_error_response" 'ADMIN_AUTHENTICATION_REQUIRED' "operator auth error response"

frontend_response="$(curl -fsS "${FRONTEND_URL}/")" \
  || fail "frontend is not reachable"
check_contains "$frontend_response" 'AI Repo Fintech Lab' "frontend HTML"

echo "PASS: MVP local smoke completed"
