#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

info() {
  echo "- $1"
}

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  fail "scripts/check-dev-rules.sh must run inside a git worktree"
fi

BASE_REF="${AI_REPO_DEV_RULES_BASE:-}"
if [[ -z "$BASE_REF" ]]; then
  if git rev-parse --verify origin/main >/dev/null 2>&1; then
    BASE_REF="$(git merge-base HEAD origin/main)"
  elif git rev-parse --verify HEAD~1 >/dev/null 2>&1; then
    BASE_REF="HEAD~1"
  else
    BASE_REF="HEAD"
  fi
fi

changed_files=()
while IFS= read -r changed_file; do
  [[ -n "$changed_file" ]] && changed_files+=("$changed_file")
done < <(
  {
    git diff --name-only "$BASE_REF"...HEAD 2>/dev/null || git diff --name-only "$BASE_REF" HEAD
    git diff --name-only
    git diff --cached --name-only
    git ls-files --others --exclude-standard
  } | sort -u
)

if [[ ${#changed_files[@]} -eq 0 ]]; then
  info "no changed files detected"
  exit 0
fi

matches_any() {
  local pattern
  for pattern in "$@"; do
    printf '%s\n' "${changed_files[@]}" | grep -Eq "$pattern" && return 0
  done
  return 1
}

require_docs_for() {
  local label="$1"
  shift
  local doc_patterns=(
    '^README\.md$'
    '^docs/adr/'
    '^docs/progress/'
    '^docs/releases/'
    '^docs/testing/'
    '^docs/frontend/'
    '^wiki-drafts/'
    '^issue-drafts/'
  )

  if matches_any "$@" && ! matches_any "${doc_patterns[@]}"; then
    fail "$label changes require a README/docs/progress/release/wiki/issue update"
  fi
}

require_docs_for "backend" '^src/main/java/' '^src/test/java/' '^build\.gradle$' '^settings\.gradle$'
require_docs_for "database" '^src/main/resources/db/migration/' '^src/main/resources/db/postgresql/'
require_docs_for "frontend" '^frontend/src/' '^frontend/e2e/' '^frontend/package(-lock)?\.json$' '^frontend/playwright\.config\.'
require_docs_for "CI or script" '^\.github/workflows/' '^scripts/'

if matches_any '^src/main/java/' && ! matches_any '^src/test/java/'; then
  fail "backend production changes require a matching src/test/java update"
fi

if matches_any '^frontend/src/' && ! matches_any '^frontend/src/.*\.test\.(ts|tsx)$' '^frontend/e2e/'; then
  fail "frontend source changes require a component or E2E test update"
fi

if matches_any '^src/main/resources/db/migration/' && ! matches_any '^docs/adr/'; then
  fail "database migration changes require an ADR update"
fi

info "dev rules check passed for ${#changed_files[@]} changed file(s)"
