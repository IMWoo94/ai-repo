# End-User JWT Authentication & Wallet Ownership — Design

## 배경과 목표

진단(2026-06-05)에서 확정된 CRITICAL 결함을 해소한다: 지갑 money-movement/query 엔드포인트가 완전히 미인증이고(`SecurityConfig`의 `anyRequest().permitAll()`), 서비스 레이어에 principal이 없어 누구나 walletId만 알면 남의 지갑을 조작/조회할 수 있다(IDOR). walletId는 순차(`wallet-001`, `wallet-002`)라 enumeration도 쉽다.

소유권 데이터 모델은 **이미 존재**한다: `wallet_accounts.member_id` → `members` FK. 빠진 것은 (1) 요청자가 누구인지 식별하는 인증, (2) 인증된 member가 그 지갑을 소유하는지 검증하는 인가다. 이 작업은 그 둘을 연결한다.

**핵심 목표:** 인증된 member만 자신이 소유한 지갑을 조작/조회할 수 있다. `member-002`의 토큰으로 `wallet-001`(member-001 소유)에 접근하면 403.

## 범위

### 포함
- `POST /api/v1/auth/tokens`: memberId로 JWT 발급 (비밀번호 없음)
- `/api/v1/wallets/**`를 JWT 인증으로 보호 (Spring Security OAuth2 Resource Server, HMAC 대칭키)
- 인증된 memberId를 WalletCommandService/QueryService로 전달, 도메인에서 소유권 검증 → 불일치 시 403
- 프론트엔드 로그인 폼 + 토큰 저장 + Authorization 헤더 + 인증된 member의 지갑으로 고정
- 백엔드/프론트 테스트, ADR/progress/issue-draft/release/wiki 동기화

### 제외 (의도적)
- 비밀번호 저장/해싱/회원가입/비밀번호 정책 — 토큰 발급은 member 존재·활성만 확인
- userId를 audit/ledger/outbox 증적에 전파 — 진단 백로그 #3으로 분리(스키마 마이그레이션 동반)
- App.tsx 컴포넌트 분해 — 별도 작업
- refresh token, 토큰 폐기/blocklist

### 보안 경계 (명시)
비밀번호 없이 memberId만으로 토큰을 발급하므로, 이 작업은 **인증된 사용자 간 격리(IDOR 차단)는 증명하되 신원 위조 방지는 범위 밖**인 학습용 설계다. 실제 운영에서는 토큰 발급 앞단에 자격증명 검증(비밀번호/OIDC)이 필요하다. 이 한계를 ADR에 명시한다.

## 아키텍처

```
프론트(App.tsx)
  로그인 폼: memberId  ──POST /api/v1/auth/tokens──►  [체인1: permitAll]  ──►  AuthTokenService
  sessionStorage: JWT  ◄───────── { token, memberId, expiresAt } ──────────────────┘
  Authorization: Bearer ─►  [체인3: /wallets/** JWT 필수]  ─► 컨트롤러(subject=memberId)
                                                              ─► WalletCommandService(memberId, walletId, …)
                                                              ─► WalletAccessPolicy.requireOwnership
                                                                  (wallet.memberId == memberId? 아니면 403)
운영 API  ─►  [체인2: 기존 AdminHeaderAuthenticationFilter + role]  (변경 없음)
```

### Security 필터 체인 (다중 SecurityFilterChain, @Order 분리)

| 순서 | 매처 | 정책 |
| --- | --- | --- |
| 1 | `/api/v1/auth/**` | `permitAll` |
| 2 | 운영 API (기존 admin 매처) | 기존 admin header 필터 + role — **변경 없음** |
| 3 | `/api/v1/wallets/**` | OAuth2 Resource Server JWT 인증 필수 |
| 4 | 그 외 (actuator health 등) | `permitAll` |

기존 admin 체인은 건드리지 않고 지갑용 JWT 체인을 추가한다. `JwtDecoder`/`JwtEncoder` 빈은 HMAC-SHA256 대칭키로 구성한다.

## 컴포넌트

### 토큰 발급
| 컴포넌트 | 책임 | 레이어 |
| --- | --- | --- |
| `AuthTokenController` | `POST /auth/tokens` | api |
| `AuthTokenRequest` / `AuthTokenResponse` | 요청/응답 record | api |
| `AuthTokenService` (인터페이스) | memberId 검증 + 토큰 발급 위임 | application |
| `JwtAuthTokenService` (구현) | Nimbus `JwtEncoder`로 서명, member 활성 검증 | infra |
| `AuthTokenProperties` | secret, TTL env 바인딩 | application(config) |
| `MemberNotActiveException` 등 | 발급 거부 사유 | application |

**발급 동작:** memberId 존재·`ACTIVE` 확인(`findMember`) → 없으면 404, 비활성 409 → JWT 생성(`subject=memberId`, `iat`, `exp=now+TTL`) → 응답.

### 인증·인가
| 컴포넌트 | 책임 | 레이어 |
| --- | --- | --- |
| `SecurityConfig`에 지갑 JWT 체인 추가 | 기존 admin 체인은 그대로 두고 `@Order`로 auth/wallet SecurityFilterChain 빈 추가 + JwtDecoder 빈 | api/config |
| `WalletCommandController`/`WalletQueryController` | `@AuthenticationPrincipal Jwt`에서 subject 추출 후 서비스에 전달 | api |
| `WalletCommandService`/`WalletQueryService` | 시그니처에 memberId 추가 | application |
| `WalletAccessPolicy.requireOwnership` | wallet.memberId == memberId 검증 | application |
| `WalletAccessDeniedException` → 403 `WALLET_ACCESS_DENIED` | 소유권 위반 | application + api handler |

**시그니처 변경:**
- `charge(String memberId, String walletId, WalletChargeCommand)`
- `transfer(String memberId, String sourceWalletId, WalletTransferCommand)` — **source만** 소유권 검증(target은 타인 지갑 허용)
- query balance/transactions에도 memberId 추가, 조회 소유권 검증

## 데이터 흐름

```
로그인:  memberId ──► JWT(subject=memberId, exp)
요청:    Bearer JWT ──► 컨트롤러 subject 추출 ──► 서비스(memberId, walletId, …)
검증:    wallet.memberId == memberId ? 통과 : WalletAccessDeniedException(403)
적용:    기존 charge/transfer/query 로직 (audit/ledger/outbox 증적은 변경 없음)
```

audit/ledger/outbox는 operation-centric 그대로 유지한다. userId 증적 전파는 후속 #3.

## 에러 처리

| 상황 | 응답 |
| --- | --- |
| 토큰 없음/만료/서명 불일치 (`/wallets/**`) | 401 (OAuth2 resource server 기본) |
| 없는 memberId로 발급 | 404 `MEMBER_NOT_FOUND` |
| 비활성 memberId로 발급 | 409 |
| 인증됐으나 비소유 지갑 접근 | 403 `WALLET_ACCESS_DENIED` |

## 테스트 전략 (TDD)

### 백엔드
| 테스트 | 검증 |
| --- | --- |
| `JwtAuthTokenService` 단위 | memberId → 유효 JWT(subject/exp), 비활성 member 거부 |
| `AuthTokenController` | 200 + 토큰, 없는 member 404 |
| `WalletCommandController` 보안 | JWT 없으면 401 / **member-002 토큰으로 wallet-001 charge·transfer·조회 → 403** / 본인 지갑 → 200 |
| `WalletAccessPolicy` 단위 | 소유 일치/불일치 |
| transfer 보안 | source 비소유 → 403, target 타인 → 허용 |
| 시나리오 테스트 | 인증 헤더 추가, 기존 흐름 유지 |

**핵심 증명 테스트:** member-002 토큰으로 wallet-001(member-001 소유)에 charge/transfer/조회 → 전부 403. IDOR 차단 증명.

### 프론트엔드
- 로그인 폼: memberId 입력 → `POST /auth/tokens` → sessionStorage 저장
- 모든 wallet 요청에 `Authorization: Bearer`
- walletId 자유 입력 필드 제거 → 로그인 member의 지갑으로 고정
- `App.test.tsx` + `wallet-flow.spec.ts` 인증 흐름으로 갱신
- App.tsx 분해는 이번 범위 밖

## 문서

- ADR 0058: End-User JWT Authentication & Wallet Ownership (보안 경계 명시)
- progress 0069, issue-draft 0068
- release notes, wiki draft 동기화
- DB 마이그레이션 없음(스키마 변경 없음) → schema.sql 변경 불필요

## dev-rules 영향

- `src/main/java` 변경 → `src/test/java` 동반(설계 포함)
- `frontend/src` 변경 → component + e2e 테스트 동반(설계 포함)
- DB 마이그레이션 없음 → schema/repository 동기화 불필요
- ADR/progress/release/wiki 동기화 포함
