# [Docs] Unreleased 릴리스 후보 문서 추가

## 배경

문서 검증 감사에서 `v0.6.0` 이후 병합된 변경분을 추적하는 릴리스 후보 문서가 없다는 P1 gap이 확인되었다.

1차 MVP 출시를 진행하려면 “현재 main이 어디까지 준비되었고, 어떤 검증을 통과해야 하며, 무엇이 blocker인지”를 릴리스 후보 문서로 고정해야 한다.

## 목표

- `docs/releases/unreleased.md`를 추가한다.
- `v0.6.0` 이후 누적 변경분을 후보 범위로 정리한다.
- MVP 출시 판단 기준, 검증 게이트, 알려진 제약, 출시 전 blocker를 문서화한다.
- README와 progress/issue draft index에서 문서를 찾을 수 있게 연결한다.

## 범위

- 릴리스 후보 문서 추가
- 진행 흔적 문서 추가
- README release history 링크 갱신
- progress/issue draft index 갱신

## 범위 제외

- 실제 release tag 발행
- GitHub Release 생성
- version bump
- GitHub Wiki 반영

## 인수 조건

- [x] `docs/releases/unreleased.md`가 있다.
- [x] `v0.6.0` 이후 주요 변경분이 후보 범위로 정리된다.
- [x] MVP 출시 판단 기준과 검증 명령이 명시된다.
- [x] 출시 전 blocker와 후속 후보가 분리된다.
- [x] README, progress, issue draft index가 문서를 연결한다.

## 검증

- `rg -n "unreleased|Release Candidate" README.md docs issue-drafts`
- `git diff --check`

## 리스크와 트레이드오프

- `unreleased` 문서는 실제 release note가 아니라 후보 기준선이다.
- 실제 tag 발행 전에는 version 이름과 포함 범위를 다시 확정해야 한다.
- Wiki 반영은 별도 작업으로 분리해 GitHub 저장소 문서와 Wiki의 책임을 유지한다.
