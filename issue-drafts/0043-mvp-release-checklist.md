# [Docs] MVP release checklist 추가

## 배경

`docs/releases/unreleased.md`는 tag 이름과 version bump 기준 결정, 실제 버전 릴리스 노트 승격을 출시 전 blocker로 남겨두고 있다.

1차 MVP 출시를 진행하려면 tag/version 정책, 필수 검증 명령, Wiki sync, GitHub Release 생성 순서를 release PR 전에 고정해야 한다.

## 목표

- MVP release checklist를 추가한다.
- 다음 release 후보를 `v0.7.0`으로 명시한다.
- Release PR에서 수행할 version bump와 release note 승격 기준을 문서화한다.
- 필수 검증 명령과 GitHub 상태 확인 항목을 체크리스트로 남긴다.

## 범위

- `docs/releases/mvp-release-checklist.md` 추가
- `docs/releases/unreleased.md` 갱신
- README release note 링크 갱신
- progress/issue draft 흔적 추가

## 범위 제외

- 실제 `build.gradle` version bump
- `docs/releases/v0.7.0.md` 생성
- Git tag 생성
- GitHub Release 생성

## 인수 조건

- [x] 다음 tag 후보가 `v0.7.0`으로 명시된다.
- [x] Gradle version bump 기준이 `0.7.0`으로 명시된다.
- [x] release PR 필수 검증 명령이 문서화된다.
- [x] Wiki sync와 GitHub Release 발행 순서가 문서화된다.

## 검증

- `rg -n "v0.7.0|MVP Release Checklist|mvp-release-checklist" README.md docs issue-drafts`
- `git diff --check`

## 리스크와 트레이드오프

- 이번 작업은 release를 발행하지 않는다.
- 실제 release PR에서 포함 범위와 known limitations를 다시 확인해야 한다.
