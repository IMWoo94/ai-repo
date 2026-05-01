# 0000. 하네스와 문서 기반

## 스펙 목표

- Java 25와 Spring Boot 기반의 핀테크 학습 레포를 만든다.
- 금융권 서비스처럼 기능보다 정책, 정합성, 감사 가능성, 테스트, 릴리스 흐름을 먼저 세운다.
- README, ADR, Wiki draft, Issue/PR 템플릿의 역할 경계를 분리한다.

## 완료 결과

- README에 프로젝트 배경, 목표, 기술 기준, 하네스 운영 모델, 브랜치/PR 규칙, 테스트 원칙을 정리했다.
- ADR을 중요한 결정의 source of truth로 정했다.
- Wiki draft를 도메인 규칙, 개발 워크플로우, 하네스 역할 문서로 구성했다.
- Issue draft를 단계별 작업 후보로 관리하기 시작했다.
- Claude Code 스킬을 Codex에서도 활용할 수 있도록 스킬 동기화 흐름을 정리했다.

## 검증

- 문서 구조가 `README.md`, `docs/adr/`, `wiki-drafts/`, `issue-drafts/`로 분리되어 있다.
- 이후 기능 작업이 모두 Issue draft, ADR, Wiki draft, 테스트와 함께 진행되었다.

## 남은 일

- 실제 GitHub Wiki에 `wiki-drafts/`를 동기화하는 운영 방식을 확정해야 한다.
- GitHub Release와 tag 발행 기준을 실제로 적용해야 한다.
- 하네스 역할별 체크리스트를 PR 템플릿과 더 강하게 연결해야 한다.

## 관련 문서

- `README.md`
- `docs/adr/0001-documentation-source-of-truth.md`
- `docs/adr/0002-test-strategy.md`
- `wiki-drafts/Harness-Roles.md`
- `wiki-drafts/Development-Workflow.md`
