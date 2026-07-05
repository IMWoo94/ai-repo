# ADR-0070: Input and Response Boundary Hardening

## 상태

Accepted

## 배경

지갑 command/응답 경계에 검증되지 않은 3개 지점이 있었다.

1. **description 길이**: `charge`/`transfer` 요청의 `description`은 `required(...)`로 null만 막고 길이는 검증하지 않았다. `operation` 관련 테이블의 `description` 컬럼은 `VARCHAR(255)`이므로 256자 이상이 들어오면 서비스/영속화 단계에서 DB 제약 위반으로 500이 났다. 잘못된 입력은 경계에서 400으로 막아야 한다.
2. **지갑 401 본문**: `walletSecurityFilterChain`(Order 4)에는 `authenticationEntryPoint`가 없어, JWT가 없거나 유효하지 않으면 Spring Security 기본 401(빈 본문)이 나갔다. 프론트가 응답 본문을 `response.json()`으로 파싱하다 빈 본문에서 `SyntaxError`를 던져 사용자에게 인증 오류 대신 깨진 에러가 노출됐다. admin/broker chain은 이미 JSON error handler로 401 형태를 통일해 두었다.
3. **last_error 절단**: outbox 실패 기록(`markOutboxEventFailed`/`markClaimedOutboxEventFailed`)이 `last_error VARCHAR(255)` 컬럼에 예외 메시지를 그대로 썼다. 긴 스택/메시지가 들어오면 실패 기록 UPDATE 자체가 DB 제약 위반으로 실패해, 실패 사유를 남기지도 못하고 claim이 꼬였다.

## 결정

세 경계를 각각 최소·외과적으로 하드닝한다.

| 항목 | 결정 |
| --- | --- |
| description 길이 | `WalletCommandController`에 `MAX_DESCRIPTION_LENGTH = 255` 상수와 `description(...)` 헬퍼를 두고, 255자 초과 시 기존 `InvalidWalletOperationException`을 던진다(`WalletApiExceptionHandler`가 400 `INVALID_WALLET_OPERATION`으로 매핑) |
| 지갑 401 본문 | broker/admin과 동일 패턴의 `WalletSecurityErrorHandler`(`AuthenticationEntryPoint`)를 추가해 `{"code","message","timestamp"}` JSON으로 `WALLET_AUTHENTICATION_REQUIRED` 401을 쓴다. `walletSecurityFilterChain`의 `exceptionHandling`과 `oauth2ResourceServer` 양쪽에 entrypoint로 연결 |
| last_error 절단 | `JdbcOutboxRelayRepository`에 널 안전한 package-private static `truncateLastError(...)`(255자 substring)를 두고 두 실패 기록 UPDATE의 `last_error` 바인딩에 적용 |

## 트레이드오프

### 장점

- 잘못된 입력과 미인증 요청을 경계에서 400/401로 명확히 막고, admin/broker와 동일한 JSON error 형태로 통일해 프론트 파싱이 안정된다.
- outbox 실패 기록이 메시지 길이와 무관하게 항상 성공해 claim 회수/재시도 흐름이 끊기지 않는다.

### 비용

- description 길이 한도(255)와 last_error 절단 한도(255)가 각각 `VARCHAR(255)` 스키마 값과 코드 상수로 이원화된다. 스키마가 바뀌면 두 상수도 함께 조정해야 한다.
- last_error를 절단하면 원본 예외 메시지 뒷부분이 유실될 수 있다(전체 스택은 애플리케이션 로그에 남는 것을 전제).

## 대안

- description 길이를 Bean Validation(`@Size`)으로 검증: 요청 record에 애노테이션과 `@Valid`를 도입해야 하고, 현재 command controller는 수동 `required(...)` 검증 패턴을 쓰므로 일관성상 헬퍼 방식을 택했다.
- 지갑 401을 controller/`@RestControllerAdvice`에서 처리: 인증 예외는 필터 단계에서 발생하므로 advice가 잡지 못한다. admin/broker와 같은 entrypoint 축이 정석이다.
- last_error 컬럼을 `TEXT`로 확장: 스키마 마이그레이션이 필요하고 실패 메시지에 무한 길이를 허용하는 것은 과하다. 경계 절단이 더 작은 변경이다.
