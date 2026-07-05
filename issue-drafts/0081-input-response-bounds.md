# 미검증 입력/응답 경계 하드닝 (#147)

## 증상

1. charge/transfer의 `description`이 256자 이상이면 `VARCHAR(255)` 제약 위반으로 500이 난다(경계에서 막지 않음).
2. 지갑 API에 JWT가 없거나 유효하지 않으면 Spring Security 기본 401이 빈 본문으로 나가고, 프론트가 `response.json()`으로 파싱하다 `SyntaxError`를 던진다.
3. outbox 실패 기록의 `last_error`가 `VARCHAR(255)`를 넘기면 실패 기록 UPDATE 자체가 실패해 사유를 남기지 못한다.

## 결정

- description 255자 초과 → `InvalidWalletOperationException`(400 `INVALID_WALLET_OPERATION`).
- `WalletSecurityErrorHandler`로 `WALLET_AUTHENTICATION_REQUIRED` JSON 401 반환(admin/broker와 동일 형태).
- `JdbcOutboxRelayRepository.truncateLastError(...)`로 last_error 255자 절단(널 안전).
- 프론트 `parseError(response)`로 빈 본문 401을 친절한 인증 오류로 변환.

## 검증

- `./gradlew test --tests '*WalletCommandControllerTest' --tests '*WalletQueryControllerTest' --tests '*JdbcOutboxRelayRepositoryLastErrorTest'`
- `npm --prefix frontend run test`
- description 256자 → 400, 미인증 → JSON 401, 긴 last_error 절단 단위 테스트, 빈 본문 401 프론트 테스트.
