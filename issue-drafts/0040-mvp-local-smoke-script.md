# [Chore] MVP 로컬 smoke 스크립트 추가

## 배경

`docs/releases/unreleased.md`는 릴리스 PR에 로컬 시연 smoke 결과를 첨부해야 한다고 명시한다. 하지만 현재는 이미 실행 중인 백엔드와 프론트를 빠르게 확인하는 스크립트가 없어 수동 curl 절차에 의존한다.

## 목표

- 이미 실행 중인 Spring Boot와 Vite 서버를 대상으로 MVP smoke를 수행한다.
- 사용자 API, 운영자 API 인증 계약, 프론트 HTML 응답을 확인한다.
- local test guide와 release candidate notes에 실행 명령을 연결한다.

## 범위

- `scripts/mvp-local-smoke.sh` 추가
- `docs/testing/local-test-guide.md` 갱신
- `docs/releases/unreleased.md` 갱신
- 진행 흔적 문서 추가

## 범위 제외

- 서버 자동 기동/중지
- actuator 도입
- 배포 환경 smoke
- GitHub Actions job 추가

## 인수 조건

- [x] 백엔드 지갑 잔액 API가 응답하지 않으면 실패한다.
- [x] 운영자 manual review API가 local admin header로 응답하지 않으면 실패한다.
- [x] 잘못된 admin token 응답에 `ADMIN_AUTHENTICATION_REQUIRED`가 없으면 실패한다.
- [x] 프론트 HTML에 `AI Repo Fintech Lab`이 없으면 실패한다.
- [x] 명령과 환경 변수 override 방법이 문서화된다.

## 검증

- `scripts/mvp-local-smoke.sh`
- `git diff --check`

## 리스크와 트레이드오프

- 이 스크립트는 이미 실행 중인 서버를 확인한다. 서버 기동과 종료까지 책임지지 않는다.
- CI 회귀 검증은 기존 Gradle, frontend unit/build/E2E, PostgreSQL scenario job이 담당한다.
