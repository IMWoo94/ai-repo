# 0038. Unreleased Release Candidate Notes

## 스펙 목표

- `v0.6.0` 이후 누적 변경분을 릴리스 후보 문서로 추적한다.
- 1차 MVP 출시 판단 기준과 검증 게이트를 명확히 한다.
- 출시 전 blocker와 후속 후보를 분리한다.

## 완료 결과

- `docs/releases/unreleased.md`를 추가했다.
- 후보 범위에 시나리오 테스트, React 화면, 운영자 콘솔, 운영 API 보안, PostgreSQL scenario CI를 정리했다.
- 릴리스 후보 검증 명령과 GitHub Actions gate를 명시했다.
- 현재 MVP 출시 전 blocker와 후속 개선 후보를 분리했다.
- README, progress index, issue draft index에서 release candidate 문서를 찾을 수 있게 연결했다.

## 검증

- `rg -n "unreleased|Release Candidate" README.md docs issue-drafts`
- `git diff --check`

## 남은 일

- 실제 tag 발행 전 `unreleased`를 버전 릴리스 노트로 승격한다.
- GitHub Wiki에 MVP 개요와 테스트 전략 요약을 반영한다.

## 관련 문서

- `docs/releases/unreleased.md`
- `docs/releases/v0.6.0.md`
- `docs/reviews/current-validation-documentation-audit.md`
- `issue-drafts/0038-unreleased-release-candidate-notes.md`
