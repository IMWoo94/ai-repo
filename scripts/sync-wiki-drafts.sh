#!/usr/bin/env bash
set -euo pipefail

SOURCE_DIR="${1:-wiki-drafts}"
TARGET_DIR="${2:-}"

fail() {
  echo "FAIL: $1" >&2
  exit 1
}

if [[ -z "$TARGET_DIR" ]]; then
  fail "usage: scripts/sync-wiki-drafts.sh <source-dir> <target-wiki-checkout>"
fi

if [[ ! -d "$SOURCE_DIR" ]]; then
  fail "source directory not found: ${SOURCE_DIR}"
fi

mkdir -p "$TARGET_DIR"

synced_count=0
while IFS= read -r source_file; do
  file_name="$(basename "$source_file")"

  if [[ "$file_name" == "README.md" ]]; then
    continue
  fi

  cp "$source_file" "${TARGET_DIR}/${file_name}"
  synced_count=$((synced_count + 1))
  echo "synced ${file_name}"
done < <(find "$SOURCE_DIR" -maxdepth 1 -type f -name '*.md' | sort)

if [[ "$synced_count" -eq 0 ]]; then
  fail "no wiki draft markdown files were synced"
fi

echo "PASS: synced ${synced_count} wiki draft files into ${TARGET_DIR}"
