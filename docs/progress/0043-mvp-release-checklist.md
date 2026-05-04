# 0043. MVP Release Checklist

## 스펙 목표

- `unreleased` 후보를 실제 1차 MVP release로 승격하기 위한 tag/version 기준을 고정한다.
- Release PR 전에 실행해야 하는 명령과 산출물을 체크리스트로 연결한다.
- `v0.7.0`을 다음 MVP release tag 후보로 명시한다.

## 완료 결과

- `docs/releases/mvp-release-checklist.md`를 추가했다.
- 다음 release note 후보를 `docs/releases/v0.7.0.md`로 정의했다.
- 다음 Git tag 후보를 `v0.7.0`으로 정의했다.
- Gradle version bump 기준을 `0.7.0`으로 정의했다.
- Release PR 필수 검증 명령, Wiki sync, GitHub Release 발행 순서를 문서화했다.
- README와 `unreleased` release candidate notes에서 체크리스트를 연결했다.

## 검증

- `rg -n "v0.7.0|MVP Release Checklist|mvp-release-checklist" README.md docs issue-drafts`
- `git diff --check`

## 남은 일

- 실제 release PR에서 `docs/releases/v0.7.0.md`를 생성하고 `build.gradle` version을 `0.7.0`으로 bump한다.
- Release PR merge 후 tag와 GitHub Release를 발행한다.

## 관련 문서

- `docs/releases/mvp-release-checklist.md`
- `docs/releases/unreleased.md`
- `issue-drafts/0043-mvp-release-checklist.md`
