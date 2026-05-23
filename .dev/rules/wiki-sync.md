---
category: concern
triggers:
  - "docs/adr/*.md"
  - "docs/progress/*.md"
  - "docs/releases/*.md"
  - "src/main/java/**/*.java"
  - "frontend/src/**/*.tsx"
depends_on:
  - documentation-sync
---

# Wiki Sync Rule

Wiki draft는 포트폴리오형 설명 문서다. 코드와 ADR은 source of truth이고 Wiki는 독자가 따라가기 쉬운 요약을 제공한다.

## Checklist

- 도메인 규칙이나 상태 전이가 바뀌면 `wiki-drafts/Domain-Rules.md`를 갱신한다.
- 테스트 시나리오가 바뀌면 `wiki-drafts/QA-Scenarios.md`를 갱신한다.
- 아키텍처 결정이나 다음 후보가 바뀌면 `wiki-drafts/Architecture-Decisions.md`를 갱신한다.
- 릴리스 후보 범위가 바뀌면 `wiki-drafts/Release-Notes.md`를 갱신한다.
- 과거 ADR/progress의 당시 기록은 소급 수정하지 않고 최신 요약 문서에서 우선순위를 명시한다.
