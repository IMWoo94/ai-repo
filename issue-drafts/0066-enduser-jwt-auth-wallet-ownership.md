# End-User JWT Authentication and Wallet Ownership

## 배경

`/api/v1/wallets/**`가 인증 없이 열려 있고 서비스가 `walletId`만으로 동작해, 임의 사용자가 타인 지갑을 충전/송금/조회/원장조회할 수 있는 IDOR가 있었다. 엔드유저 인증 체계도 없었다.

## 목표

- memberId 기반 JWT로 엔드유저를 인증한다.
- `/api/v1/wallets/**`를 JWT 필수로 보호한다.
- 인증 회원이 본인 소유 지갑에만 충전/송금/조회/원장조회를 수행하도록 서비스 계층에서 소유권을 강제한다.
- 비소유자는 403, 미인증은 401로 거부한다.

## 완료 조건

- [x] `POST /api/v1/auth/tokens`가 활성 회원 memberId로 JWT를 발급하고 미존재/비활성 회원을 거부한다.
- [x] SecurityConfig가 auth/admin/wallet/fallback 다중 체인으로 `/api/v1/wallets/**`를 JWT 필수로 보호한다.
- [x] command/query/ledger 서비스가 memberId를 받아 `WalletAccessPolicy.requireOwnership`로 소유권을 강제한다(transfer는 출금 지갑만).
- [x] `WalletAccessDeniedException`이 403 `WALLET_ACCESS_DENIED`로 매핑된다.
- [x] `/api/v1/wallets/{id}/ledger-entries` IDOR가 닫힌다.
- [x] 소유권 거부/미인증 회귀 테스트가 서비스·HTTP 계층에 추가된다.
- [x] ADR, progress, release, README, wiki draft가 갱신된다.
- [ ] 프론트엔드 로그인 + Bearer 토큰 부착 (후속 Task 12-13).

## 검증 명령

```bash
./gradlew test
./gradlew scenarioTest
./gradlew postgresScenarioTest
scripts/check-dev-rules.sh
```
