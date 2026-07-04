# Frontend Login and Bearer Auth

## 배경

백엔드가 `/api/v1/wallets/**`를 JWT 필수로 보호하면서 React 화면이 토큰 없이 호출해 401을 받는다. access token은 60분 만료되나 갱신 수단이 없다.

## 목표

- memberId 로그인으로 JWT를 발급받아 `sessionStorage`에 저장한다.
- wallet 요청에 Bearer 토큰을 싣는다.
- 401 시 자동 재발급 + 1회 재시도로 만료를 처리한다.

## 완료 조건

- [x] 미로그인 시 로그인 폼, 로그인 후 wallet 섹션 노출(운영자 콘솔은 항상).
- [x] `POST /api/v1/auth/tokens`로 토큰 발급 + `sessionStorage` 저장 + rehydrate + 로그아웃.
- [x] walletId를 로그인 회원에서 파생(자유 입력 제거).
- [x] wallet 요청에 `Authorization: Bearer`.
- [x] 401 → 재발급 → 1회 재시도 → 실패 시 세션 클리어.
- [x] 컴포넌트 테스트 + E2E 갱신.
- [x] ADR/progress/release/README/wiki 갱신.

## 검증 명령

```bash
npm --prefix frontend run test
npm --prefix frontend run build
npm --prefix frontend run e2e
scripts/check-dev-rules.sh
```
