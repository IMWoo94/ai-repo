# Wiki Drafts

이 폴더는 GitHub Wiki로 옮길 문서 초안입니다.

GitHub Wiki는 역할별 사고 과정, 긴 설명, 도메인 학습 기록을 남기는 공간입니다. 중요한 결정의 source of truth는 `docs/adr`입니다.

단계별 작업 완료 흔적은 `docs/progress/`에 기록합니다. Wiki는 도메인 지식과 운영 규칙을 정리하고, Progress Reports는 실제 작업 단계의 결과와 남은 일을 추적합니다.

## 초안 목록

- `Home.md`: Wiki 첫 화면, 현재 릴리스 상태, MVP 지도
- `Domain-Rules.md`: 지갑, 돈 이동, 원장, outbox, 운영자 콘솔 도메인 규칙
- `Architecture-Decisions.md`: ADR을 읽기 쉬운 결정 지도로 요약한 문서
- `Development-Workflow.md`: 기능 작업의 시작부터 릴리스까지의 흐름
- `Harness-Roles.md`: 하네스 역할별 책임, 산출물, 체크 기준
- `QA-Scenarios.md`: MVP 시연과 회귀 검증을 위한 QA 시나리오
- `Release-Notes.md`: 현재 release candidate와 알려진 제약
- `MCP-and-Skills.md`: MCP/스킬 사용 원칙과 현재 로컬 스킬 운영 방식

## 동기화

GitHub Wiki checkout을 준비한 뒤 다음 명령으로 초안을 복사합니다.

```bash
scripts/sync-wiki-drafts.sh wiki-drafts ../ai-repo.wiki
```

이 스크립트는 `README.md`를 제외한 `wiki-drafts/*.md`를 대상 Wiki checkout에 복사합니다. 대상에서 파일을 삭제하지 않으므로 제거가 필요한 Wiki 문서는 별도 PR/리뷰 절차로 판단합니다.

## 게시 원칙

- Wiki에 게시할 때 문서 제목은 파일명과 동일하게 둡니다.
- 중요한 결정은 Wiki에만 두지 않고 반드시 ADR 링크를 연결합니다.
- PR에는 관련 Wiki 초안 또는 게시된 Wiki 링크를 남깁니다.
- 실제 GitHub Wiki에 게시한 뒤에도 원본 초안은 PR 검증을 위해 저장소에 유지합니다.
