# ADR-0060: Audit Event–Wallet Mapping 명시화

## 상태

Accepted

## 배경

ADR-0058에서 소유자 스코프 `GET /api/v1/wallets/{walletId}/audit-events`를 추가하면서, `AuditEvent`(auditEventId, operationId, type, occurredAt, detail)에 walletId가 없다는 이유로 소유권을 `ledger_entries` 조인으로 해소했다.

- Jdbc: `audit_events WHERE operation_id IN (SELECT operation_id FROM ledger_entries WHERE wallet_id = ?)`
- InMemory: 해당 지갑의 ledger operation_id 집합으로 필터

이 방식은 "감사 이벤트의 소유자"라는 개념을 원장(ledger) 의미론에 종속시킨다. ledger에 남지 않는 감사 흐름이 생기면 소유자 조회에서 사라지고(ADR-0058 비용 항목), 조회 성능도 조인에 의존한다. ADR-0058 대안 절에서 이 결합을 "비정규화로 제거 가능하나 범위가 크다"고 유보했고, progress 0068 "남은 일"에 후속으로 남겼다.

## 결정

audit-event↔wallet 매핑을 쓰기 시점에 **명시적 매핑 테이블 `audit_event_wallets(audit_event_id, wallet_id)`**로 영속화하고, 소유자 스코프 조회를 이 매핑으로 전환한다.

| 항목 | 결정 |
| --- | --- |
| 매핑 저장 | `audit_event_wallets(audit_event_id, wallet_id)` — 복합 PK, `wallet_id` 인덱스. audit_event 삽입과 동일 트랜잭션에서 기록 |
| 쓰기 경로 | charge → 1행(`walletId`), transfer → 2행(`sourceWalletId`, `targetWalletId`). 현재 감사되는 흐름은 이 둘뿐(`insertAuditEvent` 호출 지점 전수 확인) |
| 조회 경로 | Jdbc: `audit_events WHERE audit_event_id IN (SELECT audit_event_id FROM audit_event_wallets WHERE wallet_id = ?)`. InMemory: `Map<walletId, List<auditEventId>>` 유지 |
| 파라미터 없는 조회 | operator용 `getAuditEvents()`(전체)는 변경 없음 |
| 마이그레이션 | V18에서 테이블·인덱스 생성 후, 기존 행을 ledger 조인으로 역채움해 로컬 DB 호환 유지 |
| 동작 보존 | transfer 감사 이벤트가 양쪽 소유자에게 보이고 detail에 양쪽 walletId가 포함되는 ADR-0058 수용 노출을 그대로 유지 |

## 트레이드오프

### 장점

- 소유자 매핑이 ledger 의미론과 분리된다. ledger를 남기지 않는 감사 흐름이 생겨도 소유자 조회가 독립적으로 정확하다.
- 조회가 매핑 인덱스 단일 조회로 단순화된다(ledger_entries 스캔 불필요).
- transfer의 "1 operation → 2 owner" 동작이 매핑 2행으로 명시적으로 표현된다.

### 비용

- 쓰기 경로에 삽입 1~2행이 추가된다(동일 트랜잭션이라 원자성은 유지).
- 매핑과 audit_events의 정합성을 쓰기 경로가 책임진다. 현재 삽입 지점은 두 곳뿐이라 관리 비용은 낮다.
- 새 감사 흐름을 추가할 때 관련 walletId를 매핑에 함께 기록해야 한다는 규약이 생긴다.

## 대안

- **`audit_events`에 wallet_id 배열 컬럼 추가**: 조인은 없애지만 기존 테이블 스키마를 변경하고, InMemory 미러가 복잡해지며, FK·인덱스 등 표준 관계형 의미론을 활용하기 어렵다. 매핑 테이블이 transfer의 다대일(2 owner) 관계를 더 자연스럽게 표현하므로 기각.
- **조인 유지(현행)**: 변경 없음이지만 ledger 결합이 남고 ADR-0058 비용이 지속된다.
- **audit_events에 단일 wallet_id 컬럼**: transfer가 한 operation에 양쪽 지갑을 매핑해야 하는 요구를 표현할 수 없어 기각.
