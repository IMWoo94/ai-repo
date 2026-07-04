# ADR-0058: Audit Events and Operation Log Authorization

## 상태

Accepted

## 배경

`SecurityConfig`의 Order4 default chain이 `permitAll`이라 아래 3개 조회 엔드포인트가 미인증 노출됐다.

- `GET /api/v1/audit-events` — 전체 회원 `AuditEvent` bulk 노출(가장 심각)
- `GET /api/v1/operations/{operationId}/step-logs`
- `GET /api/v1/operations/{operationId}/outbox-events`

`AuditEvent`(auditEventId, operationId, type, occurredAt, detail)에는 walletId/memberId가 없다. 소유자 매핑은 `ledger_entries(operation_id, wallet_id)` 조인으로만 가능하다: `walletId → ledger_entries.operation_id → audit_events`.

## 결정

옵션 "둘 다"를 채택한다. 운영용 전체 조회는 operator로 보호하고, 사용자용 "내 감사 로그"는 소유자 스코프 신규 엔드포인트로 제공한다.

| 항목 | 결정 |
| --- | --- |
| 운영 게이팅 | 세 엔드포인트를 `SecurityConfig.OPERATIONAL_API_PATHS`에 편입해 admin chain(GET operator role)으로 보호. 미인증 → `AdminSecurityErrorHandler`가 401 |
| 접근 감사 | `AdminApiPathMatcher`에 `/api/v1/audit-events`, `/api/v1/operations` prefix를 추가해 헤더 인증 필터와 접근 감사 필터가 함께 적용되게 함 |
| 사용자 스코프 | `GET /api/v1/wallets/{walletId}/audit-events`를 wallet chain(JWT)에 추가. `WalletAccessPolicy.findOwnedQueryableWallet`로 소유권 검증 후 `audit_events WHERE operation_id IN (SELECT operation_id FROM ledger_entries WHERE wallet_id = ?)` 반환 |
| 비소유자 | 403 `WALLET_ACCESS_DENIED` (ledger-entries와 동일) |
| 포트/어댑터 | `WalletLedgerQueryRepository.findAuditEventsByWallet`를 Jdbc(조인 쿼리)/InMemory(ledger op 집합 필터) 양쪽에 구현 |
| 프론트엔드 | 유저 화면은 소유자 스코프 audit-events 호출로 교체, operator 전용 step-logs/outbox-events는 유저 evidence에서 제거 |

## 트레이드오프

### 장점

- 미인증 bulk 노출을 닫으면서도 사용자가 자기 지갑의 감사 로그를 계속 볼 수 있다.
- 운영 조회는 operator 토큰과 접근 감사 필터로 일관되게 보호된다.

### 비용

- 소유자 스코프 감사 로그는 `ledger_entries` 조인에 의존하므로 ledger에 남지 않는 operation은 사용자 화면에서 보이지 않는다(현재 감사 대상 흐름은 모두 ledger를 남기므로 무해).
- step-logs/outbox-events는 operator 전용이 되어 사용자 화면 evidence에서 사라진다. 사용자 흐름 e2e는 증거를 원장 + 소유자 스코프 감사 로그 단언으로 검증한다.
- **수용된 노출**: transfer는 operation_id 하나에 양쪽 지갑의 ledger entry가 달리므로, `TRANSFER_COMPLETED` 감사 이벤트(detail: `Transfer completed from {source} to {target}`)가 송신자·수신자 양쪽의 소유자 스코프 조회에 모두 반환되어 상대방 walletId가 노출된다. 이는 의도된 동작으로 수용한다 — 송신자는 이체 시 상대 walletId를 이미 알고 있고, 수신자가 송신자를 보는 것은 은행 입금내역과 동일한 의미론이며, walletId 자체는 소유권 검증(`WalletAccessPolicy`) 때문에 어떤 접근 권한도 부여하지 않는다. 회귀 테스트로 이 동작을 고정한다.

## 대안

- audit-events만 operator 보호(사용자 노출 없음): 가장 단순하나 사용자 감사 가시성이 사라진다.
- `audit_events`에 wallet_id 비정규화 컬럼 추가: 조인을 없애지만 스키마 마이그레이션과 쓰기 경로 변경이 필요해 범위가 크다.
