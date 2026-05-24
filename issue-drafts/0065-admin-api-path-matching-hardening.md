# Admin API Path Matching Hardening

## 배경

운영 API 인증 필터와 접근 감사 필터가 각각 같은 prefix 목록을 들고 있고, 단순 prefix 매칭은 `/api/v1/outbox-events-v2` 같은 lookalike 공개 경로까지 운영 API로 오탐할 수 있다.

## 목표

- 운영 API 경로 판정을 하나의 matcher로 중앙화한다.
- root path와 `/` 하위 segment만 운영 API로 분류한다.
- 인증 필터와 접근 감사 필터가 같은 matcher를 사용한다.
- lookalike prefix가 인증 요구 또는 감사 로그 노이즈를 만들지 않도록 테스트한다.

## 완료 조건

- [x] `AdminApiPathMatcher`가 운영 API root/sub-path만 true로 반환한다.
- [x] `AdminHeaderAuthenticationFilter`가 공통 matcher를 사용한다.
- [x] `AdminApiAccessAuditFilter`가 공통 matcher를 사용한다.
- [x] lookalike prefix 인증/감사 회귀 테스트가 추가된다.
- [x] ADR, progress, release, README, wiki draft가 갱신된다.

## 검증 명령

```bash
./gradlew test --tests '*AdminApiPathMatcherTest' --tests '*AdminHeaderAuthenticationFilterTest' --tests '*AdminApiAccessAuditFilterTest'
scripts/check-dev-rules.sh
```
