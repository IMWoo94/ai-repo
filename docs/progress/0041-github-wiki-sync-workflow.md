# 0041. GitHub Wiki Sync Workflow

## 스펙 목표

- GitHub Wiki 게시/동기화 절차를 반복 가능한 스크립트로 고정한다.
- README 권장 Wiki 구조의 `Home`, `Release-Notes` 문서 초안을 추가한다.
- Wiki checkout에 초안을 복사하는 검증 가능한 명령을 제공한다.

## 완료 결과

- `wiki-drafts/Home.md`를 추가했다.
- `wiki-drafts/Release-Notes.md`를 추가했다.
- `scripts/sync-wiki-drafts.sh`를 추가했다.
- `wiki-drafts/README.md`에 동기화 명령과 삭제 없는 sync 정책을 문서화했다.
- README의 Wiki 구조가 실제 파일 링크를 가리키도록 갱신했다.
- `docs/releases/unreleased.md`의 출시 전 blocker를 Wiki sync 스크립트 기준으로 갱신했다.

## 검증

- `scripts/sync-wiki-drafts.sh wiki-drafts <target-dir>`
- `test -f <target-dir>/Home.md`
- `test -f <target-dir>/Release-Notes.md`
- `git diff --check`

## 남은 일

- 실제 GitHub Wiki repository checkout에 동기화 후 commit/push한다.
- Wiki에서 제거가 필요한 과거 문서는 별도 리뷰 후 정리한다.

## 관련 문서

- `scripts/sync-wiki-drafts.sh`
- `wiki-drafts/Home.md`
- `wiki-drafts/Release-Notes.md`
- `wiki-drafts/README.md`
- `issue-drafts/0041-github-wiki-sync-workflow.md`
