# ADR-0057: Frontend Member Login and Bearer Token with 401 Re-issue

## 상태

Accepted

## 배경

백엔드가 `/api/v1/wallets/**`를 JWT 필수로 보호(ADR-0056)하면서, React 사용자 화면은 토큰 없이 wallet API를 호출해 401을 받게 됐다. 또한 access token은 `ai-repo.auth.jwt.ttl-minutes`(기본 60분) 후 만료되는데 갱신 수단이 없었다. 발급은 비밀번호 없이 `POST /api/v1/auth/tokens {memberId}`로 이뤄진다.

## 결정

| 항목 | 결정 |
| --- | --- |
| 로그인 | memberId 입력 폼 → `POST /api/v1/auth/tokens` → `{token, memberId, expiresAt}`를 `sessionStorage`(`ai-repo.auth`)에 저장, 마운트 시 rehydrate |
| 인증 게이트 | 미로그인 시 wallet 섹션 대신 로그인 폼 노출, 운영자 콘솔은 항상 노출(헤더 인증 유지) |
| walletId | 로그인 회원에서 파생(`member-00N`→`wallet-00N`), 자유 입력 제거; 실제 방어는 백엔드 403 |
| Bearer | wallet 요청에 `Authorization: Bearer <token>` 부착 |
| 만료/갱신 | 401 응답 시 저장된 memberId로 토큰 재발급 후 원요청 1회 재시도; 재시도도 실패하면 세션 클리어 후 로그인 폼 |
| 백엔드 변경 | 없음(password-less 재발급으로 충분) |

## 트레이드오프

### 장점

- 백엔드 변경 없이 만료를 매끄럽게 처리하고 로그인 UX가 단순하다.
- `sessionStorage`라 탭 종료 시 세션이 사라진다.

### 비용

- memberId가 세션에 있으면 사실상 재발급으로 무기한 접근이 가능해 access token 만료의 보안 효과가 약하다(데모/학습 한정 수용).
- 만료된 토큰으로 병렬 wallet 요청이 동시에 401이면 각각 재발급을 시도할 수 있다(중복 발급, 기능상 무해).

## 대안

- 백엔드 refresh token + `/auth/refresh`: 실무형이나 password-less 발급에서는 형식적이고 작업량이 크다.
- 만료 시 강제 재로그인: 가장 단순하나 60분마다 재입력이 필요해 UX가 저하된다.
