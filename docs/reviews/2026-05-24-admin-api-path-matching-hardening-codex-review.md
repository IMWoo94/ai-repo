# Admin API Path Matching Hardening Codex Review

## 검토 대상

- 범위: 현재 staged diff
- 변경 의도: 운영 API 인증 필터와 접근 감사 필터의 path prefix 중복을 공통 matcher로 중앙화하고, root 또는 `/` 하위 segment만 매칭해 lookalike prefix 오탐을 방지한다.

## 검증

리뷰 에이전트가 다음을 확인했다.

```bash
git diff --cached --stat
git diff --cached
./gradlew test --tests '*AdminApiPathMatcherTest' --tests '*AdminHeaderAuthenticationFilterTest' --tests '*AdminApiAccessAuditFilterTest'
git diff --cached --check
```

## 판정

통과. 블로커 없음.

## 보안

- 기존 `startsWith` 기반 매칭으로 인해 `/api/v1/outbox-events-v2`, `/api/v1/outbox-relay-runs-health` 같은 lookalike prefix가 운영 API로 오탐될 수 있는 위험을 segment-aware matcher로 완화했다.
- 인증 필터와 감사 필터가 같은 matcher를 사용해 drift 위험을 줄였다.
- 실제 운영 API root/sub-path는 계속 인증·감사 대상으로 유지된다.

## 로직

- `requestUri.equals(prefix) || requestUri.startsWith(prefix + "/")` 조건은 root 또는 `/` 하위 segment만 매칭한다는 요구사항과 일치한다.
- 공통 matcher 적용 범위는 `AdminHeaderAuthenticationFilter`, `AdminApiAccessAuditFilter`로 적절하다.

## 테스트

- matcher 단위 테스트가 운영 API root/sub-path와 lookalike/public path를 모두 검증한다.
- 인증 필터 테스트가 lookalike public prefix를 admin token 없이 통과하는지 검증한다.
- 감사 필터 테스트가 lookalike public prefix에서 audit record를 남기지 않고, 실제 운영 API 접근은 기록하는지 검증한다.

## 리뷰 반영

### 수용

- `AdminApiPathMatcher.isAdminApiPath(null)` 회귀 테스트를 추가했다.
- 감사 필터 lookalike 테스트의 중복 Mockito 검증을 정리했다.

### 반박

- 없음.

### 후속 과제

- Spring Security request matcher 목록과 `AdminApiPathMatcher` prefix 목록의 동기화 테스트 또는 설정 구조를 검토한다.
