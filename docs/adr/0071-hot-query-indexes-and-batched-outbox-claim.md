# ADR-0071: Hot Query Indexes and Batched Outbox Claim

## 상태

Accepted

## 배경

Issue #148은 두 가지 성능/스케일 부채를 지적했다.

- `ledger_entries`·`transaction_history`는 `wallet_id` 기준 시간순 조회가 핫 경로인데 인덱스가 없어 지갑별 원장/거래내역이 커질수록 전체 스캔이 발생한다. `operation_outbox_events`도 relay claim이 `status`로 필터하고 `occurred_at, outbox_event_id` 순으로 정렬하는데 인덱스가 없어 이벤트가 쌓이면 정렬/스캔 비용이 커진다.
- `JdbcOutboxRelayRepository.claimReadyOutboxEvents`는 claim 대상 id를 먼저 `for update skip locked`로 확보한 뒤, 각 id마다 `update ... where outbox_event_id = ?`를 **행 단위 루프**로 실행했다. 배치 크기만큼 round-trip이 발생해 batch가 커질수록 latency가 선형 증가한다.

## 결정

| 항목 | 결정 |
| --- | --- |
| 원장/거래내역 인덱스 | `ledger_entries(wallet_id, occurred_at)`, `transaction_history(wallet_id, occurred_at)` 복합 인덱스를 추가한다. |
| outbox claim 인덱스 | `operation_outbox_events(status, occurred_at, outbox_event_id)` 복합 인덱스를 추가한다. claim 쿼리의 `status` 필터와 `order by occurred_at, outbox_event_id`를 함께 커버한다. |
| 반영 위치 | 신규 Flyway 마이그레이션 `V19__add_hot_query_indexes.sql`과 통합 스키마 `db/postgresql/schema.sql` 양쪽에 `CREATE INDEX IF NOT EXISTS`로 동일하게 반영한다. |
| claim 업데이트 배치화 | claim id 루프 UPDATE를 단일 `update ... set status=PROCESSING, claimed_at, lease_expires_at, next_retry_at=null, published_at=null, last_error=null where outbox_event_id in (?, ?, ...)`로 교체한다. `for update skip locked` 선행 claim 로직과 트랜잭션 경계는 유지한다. |

배치 UPDATE는 기존 행 단위 UPDATE와 세팅하는 컬럼/값이 완전히 동일하므로 의미가 보존된다. claim은 이미 선행 `for update skip locked` select로 대상 id를 고정했으므로, 그 id 집합에 대한 단일 IN UPDATE는 동일한 행만 전이시킨다.

## 트레이드오프

### 장점

- 지갑별 원장/거래내역 조회와 outbox claim 스캔이 인덱스로 커버돼 데이터 증가에도 정렬/스캔 비용이 안정적이다.
- claim UPDATE round-trip이 배치 크기 N에서 1로 줄어 relay tick latency가 배치 크기에 비례해 늘지 않는다.

### 비용

- 인덱스 3개 추가로 해당 테이블 쓰기 시 인덱스 유지 비용과 저장 공간이 소폭 증가한다. 원장/outbox는 append 위주라 읽기 이득이 이를 상쇄한다.
- IN 절 placeholder가 배치 크기만큼 늘어나므로 매우 큰 배치에서는 쿼리 파싱 비용이 커질 수 있으나, relay batch size는 제한돼 있어 실무 범위에서 문제되지 않는다.

## 대안

- 인덱스 없이 쿼리만 유지: 데이터가 커지면 전체 스캔으로 성능이 저하돼 이슈 요구를 충족하지 못한다.
- `jdbcTemplate.batchUpdate`로 배치화: 여러 UPDATE 문을 한 번에 보내지만 여전히 문장 수가 N개다. 모든 행이 같은 값을 세팅하므로 단일 IN UPDATE가 더 단순하고 문장 수도 1이다.
- PUBLISHED outbox 행 pruning: 이번 범위에서 제외하고 별도 스케줄러/서비스로 다룬다(progress 남은 일 참고).
