# 0070. Audit Event–Wallet Mapping 명시화

## 스펙 목표

- 소유자 스코프 `GET /api/v1/wallets/{walletId}/audit-events`가 의존하던 `ledger_entries` 조인을 제거한다.
- audit-event↔wallet 매핑을 쓰기 시점에 명시적으로 영속화한다.
- ADR-0058에서 고정한 현재 동작(transfer 감사 이벤트가 송신자·수신자 양쪽에 보이고 detail에 양쪽 walletId 포함)을 정확히 보존한다.

## 완료 결과

- 매핑 테이블 `audit_event_wallets(audit_event_id, wallet_id)`를 추가했다(복합 PK, `wallet_id` 인덱스). `V18__create_audit_event_wallets.sql`.
- 마이그레이션이 기존 audit_events를 ledger 조인으로 역채움해 로컬 DB 호환성을 유지한다.
- Jdbc/InMemory 쓰기 경로의 `insertAuditEvent`/`auditEvent` 삽입 지점에서 매핑을 동일 트랜잭션(InMemory는 동일 synchronized 블록)에 기록한다: charge → 1행(`walletId`), transfer → 2행(`sourceWalletId`, `targetWalletId`). `insertAuditEvent` 호출 지점 전수 확인 결과 감사 흐름은 이 둘뿐이다.
- `findAuditEventsByWallet`를 매핑 기반으로 전환했다. Jdbc는 `audit_event_wallets` 서브쿼리, InMemory는 `Map<walletId, List<auditEventId>>` 유지. 파라미터 없는 operator용 `getAuditEvents()`는 변경하지 않았다.

## 검증

- `./gradlew test` — BUILD SUCCESSFUL (281 통과, 1 skipped, 0 실패)
- `./gradlew postgresScenarioTest` — BUILD SUCCESSFUL (Docker Testcontainers, 컨테이너에서 V18 마이그레이션 실행; 소유 지갑 한정 + 역채움 경로 테스트 통과)
- 회귀 고정: InMemory 교차 지갑 배제(`returnsWalletScopedAuditEventsForOwnerExcludingOtherWallets`), transfer 양쪽 소유자 가시성(`returnsTransferAuditEventWithCounterpartyToBothWalletOwners`), Postgres 컨테이너 소유 지갑 한정(`findAuditEventsByWalletReturnsOnlyOwningWalletEventsInRealPostgres`) + 역채움 경로(`findAuditEventsByWalletReturnsBackfilledLegacyEventInRealPostgres`).

## 남은 일

- 새 감사 흐름 추가 시 관련 walletId를 매핑에 함께 기록하는 규약을 유지한다.
- 소유자 스코프 step-logs/outbox-events가 필요하면 별도 후속 검토(0068에서 이월).

## 관련 문서

- `docs/adr/0060-audit-event-wallet-mapping.md`
- `docs/progress/0068-audit-events-authorization.md`
- `docs/releases/unreleased.md`
- GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/117
