#!/usr/bin/env bash
set -euo pipefail

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "FAIL: scripts/check-dev-rules.sh must run inside a git worktree"
  exit 1
fi

if [[ -n "${AI_REPO_DEV_RULES_BASE:-}" ]]; then
  diff_range="${AI_REPO_DEV_RULES_BASE}..HEAD"
elif [[ -n "${BASE_REF:-}" ]]; then
  diff_range="${BASE_REF}..HEAD"
elif [[ -n "${GITHUB_BASE_REF:-}" ]]; then
  git fetch origin "${GITHUB_BASE_REF}" --depth=1 >/dev/null 2>&1 || true
  diff_range="origin/${GITHUB_BASE_REF}...HEAD"
elif git rev-parse --verify origin/main >/dev/null 2>&1; then
  diff_range="$(git merge-base HEAD origin/main)..HEAD"
else
  diff_range="HEAD~1..HEAD"
fi

changed_files="$(
  {
    git diff --name-only "${diff_range}" 2>/dev/null || true
    git diff --name-only --cached
    git diff --name-only
    git ls-files --others --exclude-standard
  } | sort -u
)"

if [[ -z "${changed_files}" ]]; then
  echo "PASS: no changed files for .dev/rules check"
  exit 0
fi

has_change() {
  local pattern="$1"
  grep -E -q "${pattern}" <<<"${changed_files}"
}

missing=()

if has_change '^src/main/java/'; then
  has_change '^src/test/java/' || missing+=("src/main/java changed but no src/test/java changes were found")
fi

if has_change '^src/main/resources/db/migration/.*\.sql$'; then
  has_change '^src/main/resources/db/postgresql/schema\.sql$' || missing+=("DB migration changed but db/postgresql/schema.sql was not updated")
  has_change '^src/test/java/.*(Repository|Postgres|Scenario).*Test\.java$' || missing+=("DB migration changed but repository/PostgreSQL/scenario test changes were not found")
fi

if has_change '^frontend/src/.*\.(ts|tsx)$'; then
  has_change '^frontend/src/.*\.test\.tsx$' || missing+=("frontend/src changed but frontend component test changes were not found")
fi

if has_change '^frontend/src/.*\.(ts|tsx)$|^frontend/e2e/.*\.ts$'; then
  has_change '^frontend/e2e/' || missing+=("frontend behavior changed but frontend/e2e changes were not found")
fi

if has_change '^src/main/java/|^src/main/resources/db/migration/|^frontend/src/|^frontend/e2e/|^scripts/'; then
  has_change '^docs/adr/' || missing+=("implementation changed but docs/adr changes were not found")
  has_change '^docs/progress/' || missing+=("implementation changed but docs/progress changes were not found")
  has_change '^docs/releases/unreleased\.md$' || missing+=("implementation changed but docs/releases/unreleased.md was not updated")
  has_change '^issue-drafts/' || missing+=("implementation changed but issue-drafts changes were not found")
fi

if has_change '^docs/adr/|^docs/progress/|^docs/releases/|^src/main/java/|^frontend/src/'; then
  has_change '^wiki-drafts/' || missing+=("ADR/progress/release/source changed but wiki-drafts changes were not found")
fi

if ((${#missing[@]} > 0)); then
  echo "FAIL: .dev/rules detected missing sync points"
  for item in "${missing[@]}"; do
    echo "- ${item}"
  done
  echo
  echo "Changed files:"
  sed 's/^/- /' <<<"${changed_files}"
  exit 1
fi

echo "PASS: .dev/rules sync checks passed"
