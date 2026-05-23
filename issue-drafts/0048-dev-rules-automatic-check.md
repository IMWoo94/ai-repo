# Dev Rules Automatic Check

## 배경

코드, 테스트, ADR, progress, Wiki draft, release note가 함께 움직이는 구조에서는 변경 누락을 사람이 매번 확인하기 어렵다.

## 목표

- `.dev/rules` 기반 자동 체크를 도입한다.
- 문서, 테스트, Wiki, release note 누락을 파일 변경 기준으로 탐지한다.
- CI에서 `Dev Rules Check` job으로 실행한다.

## 완료 기준

- [x] `.dev/rules/documentation-sync.md`가 있다.
- [x] `.dev/rules/testing-gates.md`가 있다.
- [x] `.dev/rules/wiki-sync.md`가 있다.
- [x] `scripts/check-dev-rules.sh`가 로컬에서 실행된다.
- [x] GitHub Actions에서 dev rules check가 실행된다.

## 제외

- 의미 기반 정밀 분석
- 자동 수정
