---
category: concern
triggers:
  - "src/main/java/**/*.java"
  - "src/main/resources/db/migration/*.sql"
  - "frontend/src/**/*.tsx"
  - "frontend/e2e/**/*.ts"
  - "scripts/*.sh"
depends_on:
  - testing-gates
  - wiki-sync
---

# Documentation Sync Rule

코드, UI, DB migration, 운영 스크립트가 바뀌면 검증 가능한 문서 흔적도 같이 남긴다.

## Checklist

- 중요한 설계/상태 전이/권한/DB 변경은 `docs/adr/`에 ADR을 추가하거나 갱신한다.
- 단계별 완료 흔적은 `docs/progress/`에 추가하거나 `docs/progress/README.md`에 연결한다.
- 사용자 검증 경로가 바뀌면 `README.md`, `docs/testing/local-test-guide.md`, 관련 기능 문서를 갱신한다.
- 릴리스 후보 범위나 known limitation이 바뀌면 `docs/releases/unreleased.md`를 갱신한다.
- GitHub Issue draft가 필요한 작업은 `issue-drafts/`와 `issue-drafts/README.md`를 갱신한다.
