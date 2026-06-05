# 0066. End-User JWT Auth and Wallet Ownership

## 스펙 목표

- 엔드유저를 memberId 기반 JWT로 인증하고 `/api/v1/wallets/**`를 OAuth2 resource-server 필터 체인으로 보호한다.
- 인증된 회원이 본인 소유 지갑에만 충전/송금/잔액/거래내역/원장 조회를 수행하도록 서비스 계층에서 소유권을 강제한다.
- 비밀번호 없이 활성 회원 memberId로 단기 JWT를 발급하는 인증 토큰 엔드포인트를 제공한다.

## 완료 결과

- `POST /api/v1/auth/tokens`가 활성 회원 memberId로 HS256 JWT를 발급한다(`AuthTokenController`, `JwtAuthTokenService`). 미존재/비활성 회원은 404/409로 거부한다.
- `SecurityConfig`를 다중 `SecurityFilterChain`으로 재구성했다: `/api/v1/auth/**` 공개, 운영 API admin header 체인, `/api/v1/wallets/**` JWT 필수 체인, 그 외 permit-all fallback.
- `WalletAccessPolicy.requireOwnership`를 추가하고, command/query/ledger 서비스의 `charge`/`transfer`/`getBalance`/`getTransactions`/`getLedgerEntries`에 인증된 memberId를 controller→service로 전달해 소유권을 강제한다(transfer는 출금 지갑만 검사).
- 비소유자 접근을 `WalletAccessDeniedException` → HTTP 403 `WALLET_ACCESS_DENIED`로 매핑했다(이전에는 미매핑으로 500).
- `/api/v1/wallets/{id}/ledger-entries`의 소유권 미검사(IDOR)를 닫았다.
- 컨트롤러/시나리오 테스트를 JWT principal로 인증하도록 갱신하고, test-fixtures POST가 admin 체인에서 인증되도록 `AdminApiPathMatcher`에 `/api/v1/test-fixtures`를 정렬했다.
- 서비스/HTTP 계층에 소유권 거부·미인증 회귀 테스트를 추가했다.

## 개선 건수

1. 엔드유저 JWT 인증 + per-user 지갑 소유권 서비스계층 강제(IDOR 차단).
2. 잠복 결함 2건 해소: `WalletAccessDeniedException` 미매핑 500 → 403, ledger-entries 소유권 미검사 IDOR.

## 설계 메모

- 소유권 강제 위치는 컨트롤러 guard(D)가 아니라 서비스 코어 threading(A)을 선택했다(defense-in-depth, 계획서 충실). 상세는 ADR-0056.
- 비소유자 응답은 정직한 신호로 403을 택했고 404 collapse는 하지 않았다(존재 열거 구별은 수용한 tradeoff).

## 검증

- `./gradlew test` (단위/통합 255건, 0 실패)
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest` (Testcontainers)
- `./gradlew check`
- `scripts/check-dev-rules.sh`

## 남은 일

- 프론트엔드 로그인 화면 + Bearer 토큰 부착 (Task 12-13)
- JWT 만료 후 갱신(refresh) 정책
- 토큰 secret 운영 주입(현재 local 기본값 override)

## 관련 문서

- `docs/adr/0056-enduser-jwt-auth-wallet-ownership.md`
- `docs/superpowers/plans/2026-06-05-enduser-jwt-auth-wallet-ownership.md`
- `docs/superpowers/specs/2026-06-05-enduser-jwt-auth-wallet-ownership-design.md`
- `docs/releases/unreleased.md`
