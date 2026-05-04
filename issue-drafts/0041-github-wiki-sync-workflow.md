# [Docs] GitHub Wiki sync workflow 추가

## 배경

1차 MVP 출시 준비 항목에 GitHub Wiki 게시/동기화가 남아 있다. 현재 `wiki-drafts/`는 존재하지만 README 권장 Wiki 구조의 `Home`, `Release-Notes` 초안이 없고, Wiki checkout으로 복사하는 반복 가능한 스크립트도 없다.

## 목표

- `Home`, `Release-Notes` Wiki 초안을 추가한다.
- `wiki-drafts/`를 GitHub Wiki checkout으로 동기화하는 스크립트를 추가한다.
- 문서에서 삭제 없는 sync 정책과 검증 명령을 명시한다.

## 범위

- `wiki-drafts/Home.md` 추가
- `wiki-drafts/Release-Notes.md` 추가
- `scripts/sync-wiki-drafts.sh` 추가
- `wiki-drafts/README.md`, `README.md`, `docs/releases/unreleased.md` 갱신
- 진행 흔적 문서 추가

## 범위 제외

- 실제 GitHub Wiki repository push
- Wiki 문서 삭제
- Wiki 자동 CI job

## 인수 조건

- [x] README 권장 Wiki 구조의 `Home`, `Release-Notes`가 파일 링크를 가진다.
- [x] sync script가 `README.md`를 제외한 Wiki 초안을 target directory에 복사한다.
- [x] sync script는 대상 파일 삭제를 수행하지 않는다.
- [x] 검증 명령으로 `Home.md`, `Release-Notes.md` 출력 존재를 확인할 수 있다.

## 검증

- `scripts/sync-wiki-drafts.sh wiki-drafts <target-dir>`
- `test -f <target-dir>/Home.md`
- `test -f <target-dir>/Release-Notes.md`
- `git diff --check`

## 리스크와 트레이드오프

- 실제 GitHub Wiki push는 별도 release operation으로 분리한다.
- 삭제 없는 sync는 안전하지만, 오래된 Wiki 문서를 자동 제거하지 않는다.
- 제거가 필요한 문서는 별도 리뷰 후 정리한다.
