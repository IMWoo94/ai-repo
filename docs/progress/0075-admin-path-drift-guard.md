# 0075. Admin Path Drift Guard

## 스펙 목표

운영 API 경로 목록이 `SecurityConfig`(인증 chain의 `securityMatcher`)와 `AdminApiPathMatcher`(헤더 인증·접근 감사 필터)에 별도 상수로 존재해, 한쪽만 갱신하면 drift(#109 계열)가 재발할 수 있다. 두 목록이 같은 운영 API 경로 집합을 다루는지 자동으로 검증한다.

## 완료 결과

- `OperationalApiPathDriftGuardTest`를 추가해 두 상수 목록이 같은 운영 API root 집합을 다루는지 양방향으로 단언했다.
- 각 `SecurityConfig` 운영 pattern root에 대해 `AdminApiPathMatcher`가 root/sub-path는 true, lookalike prefix는 false로 판정함을 고정했다.
- 불일치 시 실패 메시지가 누락된 경로 집합을 명시하도록 했다.

## 개선 건수

1. 운영 API 경로 목록 drift 방지 회귀 테스트 추가(단일 source 부재 상태에서의 안전망).

## 검증

- `./gradlew test --tests '*OperationalApiPathDriftGuardTest' --tests '*AdminApiPathMatcherTest'`
- `./gradlew test`
- `scripts/check-dev-rules.sh`
- `git diff --check`

## 남은 일

- Spring Security matcher와 운영 API matcher를 하나의 단일 source of truth 상수로 통합
- 실제 identity/role scope 연동

## 관련 문서

- `docs/adr/0055-admin-api-path-matching-hardening.md`
- `docs/releases/unreleased.md`
