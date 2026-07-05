# 0067. Frontend Login and Bearer Auth

## 스펙 목표

- React 사용자 화면에서 memberId로 로그인해 JWT를 발급받고, wallet API 호출에 Bearer 토큰을 싣는다.
- 토큰 만료(401)를 자동 재발급 + 1회 재시도로 처리한다.

## 완료 결과

- 미로그인 시 회원 ID 로그인 폼을 노출하고, 로그인하면 `POST /api/v1/auth/tokens`로 토큰을 발급받아 `sessionStorage`(`ai-repo.auth`)에 저장한다(마운트 시 rehydrate, 로그아웃 버튼 제공).
- 활성 wallet은 로그인 회원에서 파생(`member-00N`→`wallet-00N`)하고, 자유 입력 walletId를 제거했다.
- wallet 요청(balance/transactions/ledger-entries/charges/transfers)에 `Authorization: Bearer <token>`를 부착한다.
- 401이면 저장된 memberId로 토큰을 재발급하고 원요청을 1회 재시도하며, 재시도도 실패하면 세션을 비우고 로그인 폼을 노출한다.
- 운영자 콘솔은 헤더 인증을 그대로 유지한다.
- 컴포넌트 테스트(로그인 플로우, 401 재발급)와 E2E(member-001 로그인 후 충전/송금, member-002 잔액부족)를 추가/갱신했다. 백엔드 변경 없음.

## 개선 건수

1. 엔드유저 로그인 + Bearer 토큰 부착으로 JWT 보호 wallet API를 화면에서 사용 가능하게 함.
2. 401 자동 재발급 + 재시도로 60분 access token 만료를 매끄럽게 처리.

## 검증

- `npm --prefix frontend run test` (10 통과)
- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e` (6 통과, 실제 백엔드 + 프론트)

## 남은 일

- JWT refresh token / 만료 정책 강화(현재는 password-less 재발급)
- 로그인 회원의 실제 보유 지갑 조회 API(현재는 fixture 기반 파생)

## 관련 문서

- `docs/adr/0057-enduser-login-bearer-refresh.md`
- `docs/releases/unreleased.md`
