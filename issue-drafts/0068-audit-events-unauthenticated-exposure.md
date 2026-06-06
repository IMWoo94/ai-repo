# /api/v1/audit-events가 전체 회원 감사 로그를 미인증으로 노출

## 배경

PR 106(엔드유저 JWT 인증 + 지갑 소유권)에서 `/api/v1/wallets/**`의 balance/transactions/ledger-entries에는 소유권을 강제했으나, `GET /api/v1/audit-events`는 그대로 남았다. 이 엔드포인트는 PR 106이 만든 것이 아니라 **선존 결함**이며, 코드 리뷰(#106)에서 확정됐다.

## 문제

- `WalletLedgerController.auditEvents()`는 `walletLedgerQueryService.getAuditEvents()`를 반환하는데, 이 메서드는 **memberId 파라미터도 소유권 필터도 없이 전체 회원의 AuditEvent를 반환**한다.
- 경로가 `/api/v1/wallets/**`(JWT 체인)도 `OPERATIONAL_API_PATHS`(admin 체인)도 아니므로 `SecurityConfig.defaultSecurityFilterChain`의 permit-all로 떨어진다 → **인증 없이 누구나 호출 가능**.
- 결과: 익명 호출자가 모든 회원의 operation 감사 내역(walletId, operation 유형, detail 등)을 읽을 수 있다. 프론트 `loadWalletEvidence`도 이를 사용자 화면 "감사 로그"로 노출한다.
- 소유권 모델(`WalletAccessPolicy`, 비소유자 403)이 적용된 다른 wallet read와 일관되지 않는다.

## 제안 (택1, 설계 필요)

- **A. 사용자 화면용이면 소유자 스코프化:** JWT 필수로 보호(`/api/v1/wallets/{walletId}/audit-events`로 이전하거나 체인 추가)하고, 인증된 memberId 소유 wallet/operation으로 필터링(ledger-entries와 동일 패턴). AuditEvent→owner 매핑 경로 설계 필요.
- **B. 운영용이면 admin 체인으로 이전:** `OPERATIONAL_API_PATHS` + `AdminApiPathMatcher`에 편입해 operator 전용으로 만든다.
- A/B 결정은 이 엔드포인트의 의도(사용자 "내 활동" vs 운영 관찰) 합의가 선행돼야 한다.

## 완료 조건

- [ ] `/api/v1/audit-events`(또는 후속 경로)가 미인증 접근을 거부한다.
- [ ] 비소유자/타회원 데이터가 응답에 포함되지 않음을 회귀 테스트로 고정한다.
- [ ] 프론트 `loadWalletEvidence`가 변경된 경로/인증과 정합한다.
- [ ] ADR/progress/release/wiki 동기화.

## 검증 명령

```bash
./gradlew test scenarioTest
scripts/check-dev-rules.sh
```

관련: PR #106, ADR-0056, progress 0066.
