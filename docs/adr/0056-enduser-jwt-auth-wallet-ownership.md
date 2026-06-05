# ADR-0056: End-User JWT Authentication and Wallet Ownership

## 상태

Accepted

## 배경

`/api/v1/wallets/**`는 인증 없이 접근 가능했고, command/query/ledger 서비스는 `walletId`만으로 동작했다. 그래서 임의 사용자가 타인 지갑을 충전/송금/조회/원장조회할 수 있는 IDOR가 있었다. 운영 API는 admin/operator header 인증이 있었으나 엔드유저 인증 체계는 없었다.

소유권 강제 위치로 (A) 서비스 코어에 memberId를 전달해 강제하는 방식과 (D) 컨트롤러 경계 guard / `@PreAuthorize`로 강제하는 방식을 비교했다. D는 서비스 시그니처를 바꾸지 않아 콜사이트 변경이 적지만, 소유권 불변식이 HTTP 경계에만 있어 비-HTTP 호출자에 적용되지 않고 ledger 같은 read 모델을 별도로 처리해야 한다.

## 결정

엔드유저를 memberId 기반 JWT로 인증하고, 지갑 소유권을 서비스 계층에서 강제한다.

| 항목 | 결정 |
| --- | --- |
| 토큰 발급 | `POST /api/v1/auth/tokens`, 활성 회원 memberId로 HS256 JWT 발급 |
| 보호 경로 | `/api/v1/wallets/**` OAuth2 resource-server(JWT) 필터 체인 |
| 체인 구성 | auth(공개) / 운영 admin header / wallet JWT / permit-all fallback 다중 chain |
| 소유권 강제 위치 | command/query/ledger 서비스에 memberId threading 후 `WalletAccessPolicy.requireOwnership` |
| transfer 검사 | 출금(source) 지갑 소유권만 검사, 입금 대상 지갑은 제한하지 않음 |
| 비소유자 응답 | `WalletAccessDeniedException` → HTTP 403 `WALLET_ACCESS_DENIED` |
| test-fixtures | `AdminApiPathMatcher`에 정렬해 admin 체인에서 인증되도록 일관화 |

## 트레이드오프

### 장점

- 소유권 불변식이 HTTP 경계가 아니라 use-case 서비스에 있어 비-HTTP 호출자에도 적용된다(defense-in-depth).
- `getLedgerEntries`를 포함한 모든 wallet-scoped 읽기/쓰기에 같은 기준으로 적용된다.
- 미매핑이던 `WalletAccessDeniedException`을 403으로 매핑해 IDOR 시도가 500으로 새지 않는다.

### 비용

- memberId가 command/query/ledger 서비스 시그니처를 관통해 콜사이트 변경 폭이 크다(컨트롤러 guard 대비).
- 비소유자에 403을 택해 "없는 지갑(404)"과 "남의 지갑(403)"의 존재 열거(enumeration) 구별 가능성을 수용했다(404 collapse 안 함).

## 대안

- 컨트롤러 경계 guard 또는 `@PreAuthorize` SpEL: 콜사이트 변경은 0에 가깝지만 서비스 코어 자기방어가 없고 ledger를 별도 처리해야 하며, `@PreAuthorize` 거부는 `@RestControllerAdvice`를 우회해 403/404 응답 shape 제어가 어렵다. 채택하지 않음.
