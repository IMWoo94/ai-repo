# ADR-0055: Admin API Path Matching Hardening

## 상태

Accepted

## 배경

운영 API 인증 필터와 접근 감사 필터는 같은 운영 API 경로 목록을 각자 prefix 문자열로 들고 있었다. 이 방식은 목록이 중복되어 drift가 생길 수 있고, 단순 `startsWith` 판정은 `/api/v1/outbox-events-v2`처럼 공개 경로가 운영 API prefix를 흉내 낼 때 인증·감사 대상으로 오탐될 수 있다.

운영 API 보호는 보수적이어야 하지만, 공개 API를 운영 API로 잘못 분류하면 불필요한 401/403과 감사 로그 노이즈가 생긴다. 따라서 root path와 `/`로 이어지는 하위 path segment만 운영 API로 판정하는 공통 기준이 필요하다.

## 결정

`AdminApiPathMatcher`를 추가하고, 인증 필터와 접근 감사 필터가 같은 matcher를 사용한다.

| 항목 | 결정 |
| --- | --- |
| matcher 위치 | `com.imwoo.airepo.wallet.api.AdminApiPathMatcher` |
| 매칭 기준 | 운영 API root와 `prefix + "/"` 하위 segment |
| 오탐 방지 | `prefix` 뒤에 다른 문자가 붙은 lookalike path는 false |
| 적용 대상 | `AdminHeaderAuthenticationFilter`, `AdminApiAccessAuditFilter` |
| 회귀 검증 | matcher, 인증 필터, 감사 필터 단위 테스트 |

## 트레이드오프

### 장점

- 인증과 감사의 운영 API 경로 분류가 하나의 기준으로 유지된다.
- lookalike prefix로 인한 공개 경로 오탐을 막는다.
- 새 운영 API prefix 추가 시 회귀 테스트로 root/sub-path와 lookalike path를 함께 확인할 수 있다.

### 비용

- 운영 API prefix 목록은 여전히 코드 상수에 있다.
- Spring Security request matcher 목록과 완전히 같은 source of truth는 아니므로 신규 운영 API 추가 시 두 위치를 함께 검토해야 한다.

## 검증 기준

- 실제 운영 API root path와 하위 path는 matcher가 true를 반환한다.
- lookalike prefix와 일반 wallet API는 matcher가 false를 반환한다.
- lookalike path는 admin token 없이도 인증 필터를 통과한다.
- lookalike path는 접근 감사 record를 남기지 않는다.

## 후속 작업

- Spring Security matcher와 운영 API matcher 목록을 더 강하게 동기화할 수 있는 테스트 또는 설정 구조를 검토한다.
- 실제 identity/role scope 연동 시 matcher 책임 범위를 재검토한다.
