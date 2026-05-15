# Repository Instructions

## UI Generation

- 사용자 화면 또는 UI를 생성·수정할 때는 반드시 `DESIGN-apple.md`를 참고한다.
- UI 문서나 코드 주석에는 로컬 내부 절대경로를 남기지 않고 파일명만 언급한다.

## Post-Implementation Review

- 코드 구현이 포함된 작업은 구현·테스트·문서 갱신 후 코드 리뷰 단계를 진행한다.
- 1차 리뷰는 Codex 리뷰 에이전트로 수행한다. 기본 관점은 백엔드/프론트엔드/QA·테스트/DevOps 중 변경 범위에 맞춰 선택한다.
- 2차 리뷰는 Claude Code 리뷰로 수행할 수 있다. 지정 세션 이름은 `핀테크-클로드-리뷰`이며, Claude Code 쪽 하네스가 적용된 세션에 리뷰를 요청한다.
- 리뷰 결과는 `docs/reviews/`에 Markdown으로 남긴다.
- 리뷰 반영은 `수용`, `반박`, `후속 과제`로 분류하고, 수용 항목만 구현에 반영한다.
- 리뷰 후에는 관련 테스트와 `scripts/check-dev-rules.sh`를 다시 실행한다.
- 단순 문서 오탈자처럼 코드 변경이 없는 작업은 리뷰를 생략할 수 있다.
