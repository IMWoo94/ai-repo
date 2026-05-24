# 0065. Admin API Path Matching Hardening

## 스펙 목표

운영 API 인증 필터와 접근 감사 필터가 같은 경로 판정 기준을 사용하게 하고, `/api/v1/outbox-events-v2`처럼 운영 API prefix를 흉내 내는 공개 경로가 실수로 인증·감사 대상이 되지 않도록 한다.

## 완료 결과

- `AdminApiPathMatcher`를 추가해 운영 API root path와 하위 segment만 매칭하도록 중앙화했다.
- `AdminHeaderAuthenticationFilter`와 `AdminApiAccessAuditFilter`가 동일 matcher를 사용하도록 중복 prefix 목록을 제거했다.
- lookalike prefix는 인증을 요구하지 않고 감사 로그도 남기지 않는 회귀 테스트를 추가했다.
- 실제 운영 API root/sub-path는 계속 인증·감사 대상임을 parameterized test로 고정했다.

## 개선 건수

1. 운영 API path 판정 로직 중앙화와 segment-aware hardening.
2. 인증/감사 필터 회귀 테스트 보강으로 prefix 오탐 방지 검증 자동화.

## 검증

- `./gradlew test --tests '*AdminApiPathMatcherTest' --tests '*AdminHeaderAuthenticationFilterTest' --tests '*AdminApiAccessAuditFilterTest'`
- `./gradlew check`
- `scripts/check-dev-rules.sh`
- `git diff --check`

## 남은 일

- 실제 identity/role scope 연동
- pruning 실행 이력 저장과 조회 API
- 운영 alert 화면 연결

## 관련 문서

- `README.md`
- `docs/releases/unreleased.md`
