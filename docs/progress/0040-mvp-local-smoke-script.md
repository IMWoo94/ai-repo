# 0040. MVP Local Smoke Script

## 스펙 목표

- 릴리스 시연 전 이미 실행 중인 백엔드와 프론트가 최소 연결 상태를 만족하는지 빠르게 확인한다.
- 사용자 API, 운영자 API 인증 계약, 프론트 HTML 응답을 하나의 명령으로 검증한다.
- 릴리스 후보 문서의 local smoke blocker를 실행 가능한 스크립트로 바꾼다.

## 완료 결과

- `scripts/mvp-local-smoke.sh`를 추가했다.
- 백엔드 지갑 잔액 API 응답을 확인한다.
- 운영자 manual review API가 local admin header로 응답하는지 확인한다.
- 잘못된 admin token이 `ADMIN_AUTHENTICATION_REQUIRED`를 반환하는지 확인한다.
- Vite 프론트 HTML 응답을 확인한다.
- local test guide와 unreleased release candidate notes에 smoke 명령을 연결했다.

## 검증

- `scripts/mvp-local-smoke.sh`
- `git diff --check`

## 남은 일

- Actuator health check는 후속 단계에서 MVP local smoke에 포함했다.
- smoke script가 서버를 직접 기동/중지하는 방식은 필요해질 때 별도 작업으로 분리한다.

## 관련 문서

- `scripts/mvp-local-smoke.sh`
- `docs/testing/local-test-guide.md`
- `docs/releases/unreleased.md`
- `issue-drafts/0040-mvp-local-smoke-script.md`
