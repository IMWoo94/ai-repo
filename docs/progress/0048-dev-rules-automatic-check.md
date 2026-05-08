# 0048. Dev Rules Automatic Check

## 스펙 목표

ADR, progress, Wiki, test 누락을 자동으로 탐지하는 `.dev/rules` 기반 체크를 도입한다.

## 완료 결과

- `.dev/rules/documentation-sync.md`를 추가했다.
- `.dev/rules/testing-gates.md`를 추가했다.
- `.dev/rules/wiki-sync.md`를 추가했다.
- `scripts/check-dev-rules.sh`를 추가해 변경 파일 기반 누락 검사를 실행한다.
- GitHub Actions에 `Dev Rules Check` job을 추가했다.

## 검증

- `scripts/check-dev-rules.sh`
- `./gradlew check`
- `npm --prefix frontend run test`
- `git diff --check`

## 남은 일

- false positive가 누적되면 예외 규칙이나 allowlist를 추가한다.
- 반복 누락 패턴이 생기면 `.dev/rules`를 더 세분화한다.

## 관련 문서

- `docs/adr/0039-dev-rules-automatic-sync-check.md`
- `.dev/rules/documentation-sync.md`
- `.dev/rules/testing-gates.md`
- `.dev/rules/wiki-sync.md`
