# Design: audit-events / operation-log authorization (issue 0068)

날짜: 2026-07-04
관련: issue-drafts/0068, PR #106, ADR-0056, progress 0066
결정: **옵션 "둘 다"** — 운영용 전체 조회는 operator, 사용자용 "내 감사 로그"는 소유자 스코프.

## 문제 (확정)

`SecurityConfig`의 Order4 default chain이 `permitAll`이라 아래 3개가 **미인증 노출**된다.

- `GET /api/v1/audit-events` — 전체 회원 AuditEvent (bulk 노출, 가장 심각)
- `GET /api/v1/operations/{operationId}/step-logs`
- `GET /api/v1/operations/{operationId}/outbox-events`

`AuditEvent`(auditEventId, operationId, type, occurredAt, detail)에는 walletId/memberId가 없다. 소유자 매핑은 `ledger_entries(operation_id, wallet_id)` 조인으로 가능: `walletId → ledger_entries.operation_id → audit_events`.

## 결정

### 운영 (operator) — B1.1

세 엔드포인트를 `OPERATIONAL_API_PATHS`에 편입해 admin chain(operator role)으로 보호한다.

- `SecurityConfig.OPERATIONAL_API_PATHS`에 `"/api/v1/audit-events/**"`, `"/api/v1/operations/**"` 추가.
- 미인증 → `AdminSecurityErrorHandler`가 401. operator/admin 토큰 → 200.
- `AdminApiPathMatcher`/`AdminApiAccessAuditFilter`가 자동으로 접근 감사 대상에 포함(부수 효과: 좋음).

### 사용자 (owner) — B1.2

소유자 스코프 신규 엔드포인트를 wallet chain(JWT)에 추가한다.

- `GET /api/v1/wallets/{walletId}/audit-events` → `WalletLedgerController`에 추가.
- `WalletLedgerQueryService.getAuditEvents(memberId, walletId)`:
  1. `WalletAccessPolicy.findOwnedQueryableWallet(...)`로 소유권 검증(비소유자 403 `WALLET_ACCESS_DENIED`).
  2. `walletLedgerQueryRepository.findAuditEventsByWallet(walletId)` — `audit_events WHERE operation_id IN (SELECT operation_id FROM ledger_entries WHERE wallet_id = ?)`.
- 어댑터 양쪽 구현: `JdbcWalletRepository`(조인 쿼리), `InMemoryWalletRepository`(ledger의 op 집합으로 필터).
- 기존 파라미터 없는 `getAuditEvents()`는 운영(operator) 컨트롤러 경로에서만 사용.

## 프론트엔드 (B1.2와 함께)

`App.tsx loadWalletEvidence`는 현재 유저 화면에서 `/audit-events`(전체) + `/operations/{id}/step-logs` + `/operations/{id}/outbox-events`를 호출 → 모두 operator 전용이 되면 깨진다.

- 유저 화면: `/api/v1/wallets/{walletId}/audit-events`(소유자 스코프)로 교체. operator 전용 step-logs/outbox-events는 유저 evidence에서 제거(원장/거래내역은 이미 소유자 스코프 유지).
- operator 콘솔: 기존 전체 조회 유지(operator 토큰).
- `App.test.tsx` mock 경로 갱신.

## 테스트 (TDD)

B1.1:
- `audit-events`/`operations/{id}/step-logs`/`operations/{id}/outbox-events` 미인증 → 401 (신규 회귀).
- 위 엔드포인트 operator 헤더 → 200 (기존 테스트 갱신: `WalletLedgerControllerTest.returnsAuditEventsAfterCharge` 등 operator 헤더 추가).
- 영향 파일: `WalletLedgerControllerTest`, `OperationOutboxReviewControllerTest`(line 101 무인증 호출), `WalletScenarioFlowTest`, `PostgresWalletScenarioFlowTest`.

B1.2:
- `GET /wallets/{walletId}/audit-events` 소유자 → 자기 wallet operation의 audit만.
- 비소유자 → 403 `WALLET_ACCESS_DENIED`.
- 미인증 → 401.
- 서비스 단위 테스트: 다른 wallet의 audit 미포함.

## 동기화 (dev-rules)

코드 변경이므로 완료 시: follow-up ADR(0058 audit-events authorization), progress(0068), release unreleased, wiki(Domain-Rules/Architecture-Decisions) 갱신. `scripts/check-dev-rules.sh` 통과.

## 검증 명령

```bash
./gradlew test scenarioTest
cd frontend && npm run test
scripts/check-dev-rules.sh
```
