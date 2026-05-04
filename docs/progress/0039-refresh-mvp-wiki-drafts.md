# 0039. Refresh MVP Wiki Drafts

## 스펙 목표

- 1차 MVP 기준으로 Wiki 초안을 최신화한다.
- Domain Rules가 현재 구현된 원장, outbox, 운영자 콘솔, PostgreSQL 검증 흐름을 반영하게 한다.
- README가 권장하는 QA, Architecture, MCP/Skills Wiki 초안을 추가한다.

## 완료 결과

- `wiki-drafts/Domain-Rules.md`에 현재 MVP 기준선, outbox 운영자 조치 규칙, 화면/검증 규칙을 추가했다.
- `wiki-drafts/QA-Scenarios.md`를 추가했다.
- `wiki-drafts/Architecture-Decisions.md`를 추가했다.
- `wiki-drafts/MCP-and-Skills.md`를 추가했다.
- `wiki-drafts/README.md`와 `README.md`의 Wiki 링크를 갱신했다.
- 문서 검증 감사의 Wiki 관련 gap 상태를 후속 보강 완료로 갱신했다.

## 검증

- `rg -n "QA-Scenarios|Architecture-Decisions|MCP-and-Skills|현재 MVP 기준선" README.md wiki-drafts docs/reviews`
- `git diff --check`

## 남은 일

- 실제 GitHub Wiki에 초안을 게시하거나 동기화하는 절차를 정한다.
- Release Notes Wiki 초안은 실제 버전 릴리스 노트 승격 시점에 추가한다.

## 관련 문서

- `wiki-drafts/Domain-Rules.md`
- `wiki-drafts/QA-Scenarios.md`
- `wiki-drafts/Architecture-Decisions.md`
- `wiki-drafts/MCP-and-Skills.md`
- `docs/reviews/current-validation-documentation-audit.md`
- `issue-drafts/0039-refresh-mvp-wiki-drafts.md`
