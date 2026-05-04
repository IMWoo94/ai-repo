# [Docs] MVP Wiki 초안 최신화

## 배경

문서 검증 감사에서 Wiki draft 최신화가 P1 gap으로 남아 있고, README 권장 Wiki 구조 중 `QA-Scenarios`, `Architecture-Decisions`, `MCP-and-Skills` 초안이 없다는 P2 gap이 확인되었다.

1차 MVP 출시 후보를 검증하려면 README/ADR/Progress뿐 아니라 GitHub Wiki로 옮길 수 있는 설명 문서도 현재 구현 상태와 맞아야 한다.

## 목표

- `Domain-Rules`를 현재 MVP 구현 상태에 맞게 보강한다.
- QA 시나리오, 아키텍처 결정 지도, MCP/Skills 초안을 추가한다.
- README와 Wiki draft index를 실제 파일 링크와 맞춘다.
- 문서 검증 감사의 Wiki gap 상태를 갱신한다.

## 범위

- `wiki-drafts/Domain-Rules.md` 최신화
- `wiki-drafts/QA-Scenarios.md` 추가
- `wiki-drafts/Architecture-Decisions.md` 추가
- `wiki-drafts/MCP-and-Skills.md` 추가
- `wiki-drafts/README.md`, `README.md` 링크 갱신
- `docs/reviews/current-validation-documentation-audit.md` 상태 갱신

## 범위 제외

- 실제 GitHub Wiki 게시
- Wiki 자동 동기화 스크립트
- Release Notes Wiki 초안

## 인수 조건

- [x] README 권장 Wiki 구조의 QA/Architecture/MCP 문서가 파일로 존재한다.
- [x] Domain Rules가 outbox, manual review, requeue, 운영 API 보호, PostgreSQL scenario 검증을 반영한다.
- [x] Wiki draft index가 모든 초안을 나열한다.
- [x] 문서 검증 감사에서 Wiki 최신화 상태가 갱신된다.

## 검증

- `rg -n "QA-Scenarios|Architecture-Decisions|MCP-and-Skills|현재 MVP 기준선" README.md wiki-drafts docs/reviews`
- `git diff --check`

## 리스크와 트레이드오프

- Wiki 초안은 설명 문서이므로 결정 자체는 ADR에 남긴다.
- 실제 GitHub Wiki 게시와 동기화 자동화는 별도 작업으로 분리한다.
