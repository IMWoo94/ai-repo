# MCP and Skills

이 문서는 프로젝트에서 MCP와 Codex/Claude 계열 스킬을 어떤 기준으로 사용할지 정리하는 Wiki 초안이다.

도구는 편의를 위한 실행 수단이며, 결정 근거의 source of truth는 Issue, PR, ADR, release note, Wiki 초안이다.

## 사용 원칙

| 원칙 | 설명 |
| --- | --- |
| 필요한 때만 연결 | MCP나 스킬은 작업 목적이 명확할 때만 사용한다 |
| 권한을 문서화 | 저장소, GitHub, 캘린더, 문서 도구 등 접근 권한 범위를 기록한다 |
| 결과를 PR에 남김 | 도구가 만든 결과도 코드, 테스트, 문서 diff로 검증한다 |
| 결정은 ADR에 고정 | 스킬 출력이나 대화 내용만으로 중요한 결정을 확정하지 않는다 |
| 로컬 경로 노출 최소화 | 문서에는 내부 절대 경로 대신 파일명과 역할만 남긴다 |

## 현재 사용 중인 스킬 흐름

| 스킬/흐름 | 목적 | 산출물 |
| --- | --- | --- |
| deep-interview/interview | 요구사항과 숨은 전제 정리 | README, spec, issue draft |
| agent-team | 기획자, 도메인 전문가, 개발자, QA, 릴리스 관리자 역할 설계 | harness role 문서 |
| code-standards | 구현과 테스트 작성 기준 확인 | 코드/테스트 PR |
| test-planner/test-generator/test-healer | 테스트 시나리오, 테스트 작성, 실패 수정 | unit, scenario, E2E test |
| pr-reviewer/pr-diff-summary | PR 리뷰와 변경 요약 | PR 설명과 리뷰 반영 |
| work-report | 단계별 작업 결과 요약 | `docs/progress/` |

## MCP 후보

| 후보 | 목적 | 도입 시점 |
| --- | --- | --- |
| GitHub MCP | Issue, PR, Wiki, Release 작업 자동화 | GitHub 작업량이 늘어날 때 |
| PostgreSQL MCP | 스키마 탐색, 쿼리 점검, 테스트 데이터 확인 | DB 모델이 복잡해질 때 |
| Browser/Playwright MCP | 로컬 시연과 시각적 QA 보조 | 사용자 화면 검증이 많아질 때 |
| Notion/Docs MCP | 긴 기획 문서와 회고 정리 | 외부 문서 저장소가 필요할 때 |

## 현재 로컬 운영 기준

- Claude Code용으로 작성한 스킬은 Codex에서 사용할 수 있도록 변환 설치한다.
- 작업에 사용한 스킬은 별도 Markdown 흔적으로 남긴다.
- UI 생성은 Apple 스타일 디자인 기준 파일을 참고한다.
- 실제 코드 변경은 PR, 테스트, CI로 검증한다.

## 금지 또는 주의

- MCP 연결만으로 검증 완료로 판단하지 않는다.
- 로컬 secret, token, 내부 절대 경로를 Wiki에 남기지 않는다.
- 자동 생성 문서를 ADR 없이 결정 근거로 사용하지 않는다.
- destructive command는 사용자 확인 없이 실행하지 않는다.

## 다음 후보

- GitHub Wiki 동기화 절차 자동화
- release note 생성 보조 스킬
- E2E 시연 결과 캡처 자동화
- 운영자 manual review fixture 생성 스킬 또는 스크립트
