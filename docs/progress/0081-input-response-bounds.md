# 0081. Input and Response Boundary Hardening

## 스펙 목표

- Issue #147의 미검증 경계 3건을 각각 최소·외과적으로 막는다.
- description 256자 이상 → 500이 아니라 400.
- 지갑 미인증 요청 → 빈 본문이 아니라 JSON error 401.
- outbox 실패 기록의 last_error가 컬럼 한도를 넘겨 UPDATE 자체가 실패하지 않게 절단한다.

## 완료 결과

- `WalletCommandController`에 `description(...)` 헬퍼와 `MAX_DESCRIPTION_LENGTH = 255`를 추가해 charge/transfer의 description 255자 초과를 `InvalidWalletOperationException`(400 `INVALID_WALLET_OPERATION`)으로 막았다.
- `WalletSecurityErrorHandler`(`AuthenticationEntryPoint`)를 추가하고 `walletSecurityFilterChain`의 `exceptionHandling`/`oauth2ResourceServer`에 연결해, 미인증/유효하지 않은 JWT에 `WALLET_AUTHENTICATION_REQUIRED` JSON 401을 반환한다.
- `JdbcOutboxRelayRepository`에 널 안전한 `truncateLastError(...)`(255자)를 추가하고 `markOutboxEventFailed`/`markClaimedOutboxEventFailed`의 last_error 바인딩에 적용했다.
- 프론트 `App.tsx`에 `parseError(response)` 헬퍼를 두어 빈 본문 응답에서 `SyntaxError` 대신 상태코드 기반 인증 오류 메시지를 던지도록 두 파싱 지점을 공통화했다.
- ADR-0070, ADR index, unreleased release note, issue draft, wiki draft를 갱신했다.

## 검증

- `./gradlew compileTestJava`
- `./gradlew test --tests '*WalletCommandControllerTest' --tests '*WalletQueryControllerTest' --tests '*JdbcOutboxRelayRepositoryLastErrorTest'`
- `npm --prefix frontend run test`
- `AI_REPO_DEV_RULES_BASE=origin/main bash scripts/check-dev-rules.sh` → PASS

## 남은 일

- description 길이 한도와 last_error 절단 한도가 `VARCHAR(255)` 스키마와 코드 상수로 이원화되어 있어, 스키마 변경 시 함께 조정해야 한다.

## 관련 문서

- `docs/adr/0070-input-response-boundary-hardening.md`
- `docs/releases/unreleased.md`
- `issue-drafts/0081-input-response-bounds.md`
