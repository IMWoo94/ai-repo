# ADR-0039: Dev Rules Automatic Sync Check

## 상태

Accepted

## 배경

프로젝트는 ADR, progress, Wiki draft, local test guide, release note를 함께 관리한다. 기능이 늘어나면서 코드 변경은 되었지만 문서나 테스트 흔적이 누락되는 위험이 커졌다.

Codex `check` 스킬은 `.dev/rules` 디렉터리를 기준으로 변경 누락을 검증하도록 설계되어 있으나, repo에는 아직 규칙 파일과 실행 가능한 검사 스크립트가 없었다.

## 결정

`.dev/rules`에 문서/테스트/Wiki 동기화 규칙을 추가하고, `scripts/check-dev-rules.sh`를 CI job으로 실행한다.

규칙 범위:

- `documentation-sync`: 코드, UI, DB, 운영 스크립트 변경 시 ADR/progress/release/issue draft 동기화 확인
- `testing-gates`: Java, DB migration, React, E2E 변경 시 테스트 동반 여부 확인
- `wiki-sync`: ADR/progress/release/source 변경 시 Wiki draft 갱신 여부 확인

## 트레이드오프

### 장점

- PR에서 문서와 테스트 누락을 빠르게 탐지한다.
- `check` 스킬과 CI가 같은 규칙 디렉터리를 공유한다.
- 포트폴리오형 운영 하네스의 문서 품질을 자동 게이트로 끌어올린다.

### 비용

- 단순 파일 변경 기반 검사라 false positive 가능성이 있다.
- 코드 변경 없는 작은 수정도 문서 동기화 규칙에 걸릴 수 있다.
- 규칙이 늘어나면 예외 정책을 추가로 정리해야 한다.

## 검증 기준

- `.dev/rules/*.md`에 YAML frontmatter가 있다.
- `scripts/check-dev-rules.sh`가 변경 파일을 기준으로 누락을 탐지한다.
- GitHub Actions에 `Dev Rules Check` job이 추가된다.
- 로컬에서 `scripts/check-dev-rules.sh`가 통과한다.
