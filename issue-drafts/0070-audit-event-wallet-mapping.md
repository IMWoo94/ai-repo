# Audit Event–Wallet 매핑 명시화(ledger 조인 제거)

## 배경

소유자 스코프 `GET /api/v1/wallets/{walletId}/audit-events`(ADR-0058)는 `AuditEvent`에 walletId가 없어 소유권을 `ledger_entries` 조인으로 해소했다: `audit_events WHERE operation_id IN (SELECT operation_id FROM ledger_entries WHERE wallet_id = ?)`. 이 결합은 감사 이벤트 소유자를 원장 의미론에 종속시켜, ledger를 남기지 않는 감사 흐름이 생기면 소유자 조회에서 누락된다(ADR-0058 비용 항목, progress 0068 "남은 일").

## 목표

- audit-event↔wallet 매핑을 쓰기 시점에 명시적으로 영속화해 ledger 조인 의존을 제거한다.
- ADR-0058이 고정한 현재 동작(transfer 감사 이벤트가 송신자·수신자 양쪽에 보이고 detail에 양쪽 walletId 포함)을 정확히 보존한다.

## 완료 조건

- [x] 매핑 테이블 `audit_event_wallets(audit_event_id, wallet_id)`를 추가한다(복합 PK, `wallet_id` 인덱스).
- [x] Flyway V18에서 테이블 생성 + 기존 행을 ledger 조인으로 역채움한다(로컬 DB 호환).
- [x] Jdbc/InMemory 쓰기 경로가 audit event와 동일 트랜잭션에 매핑을 기록한다(charge 1행, transfer 2행).
- [x] `findAuditEventsByWallet`를 매핑 기반으로 전환한다. operator용 `getAuditEvents()`는 불변.
- [x] InMemory 교차 지갑 배제 / transfer 양쪽 소유자 가시성 / Postgres 컨테이너 소유 지갑 한정 + 역채움 경로 테스트가 통과한다.
- [x] ADR-0060, progress 0070 기록.

## 관련 문서

- `docs/adr/0060-audit-event-wallet-mapping.md`
- `docs/progress/0070-audit-event-wallet-mapping.md`
