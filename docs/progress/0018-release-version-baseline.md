# 0018. Release Version Baseline

## 스펙 목표

- 현재 `main` 기준 기능 묶음을 첫 검증 기준선으로 고정한다.
- Gradle version, release note, ADR, GitHub Release를 하나의 기준으로 맞춘다.
- 릴리스 전 검증 결과와 알려진 리스크를 문서에 남긴다.

## 완료 결과

- Gradle version을 `0.6.0`으로 갱신했다.
- ADR-0021로 릴리스 기준선 정책을 기록했다.
- `docs/releases/v0.6.0.md` 릴리스 노트를 추가했다.
- Issue #37 기준 작업 초안을 추가했다.
- README, Wiki, Progress index에 릴리스 기준선을 연결했다.
- PR merge와 GitHub Actions 통과 후 `v0.6.0` tag와 GitHub Release를 발행한다.

## 검증

- `./gradlew check`로 전체 테스트와 빌드 검증을 수행한다.
- `docker compose config`로 로컬 PostgreSQL compose 설정을 검증한다.
- `git diff --check`로 whitespace 오류를 확인한다.
- PR merge 후 GitHub Actions `Gradle Check` 통과를 확인한다.

## 남은 일

- 실제 broker 발행은 아직 없다.
- 인증/인가와 승인 워크플로우는 아직 없다.

## 관련 문서

- `docs/adr/0021-release-version-baseline.md`
- `docs/releases/v0.6.0.md`
- `issue-drafts/0018-release-version-baseline.md`
