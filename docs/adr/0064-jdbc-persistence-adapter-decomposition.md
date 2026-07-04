# ADR-0064: JDBC Persistence Adapter Decomposition

## 상태

Accepted

## 배경

`JdbcWalletRepository`는 PostgreSQL profile에서 여러 application port를 한 번에 구현해 왔다. 시간이 지나면서 wallet command/query, ledger, outbox relay, consumer idempotency/monitoring/pruning, operational alert, admin API access audit SQL이 한 클래스에 모였다.

이 구조는 단일 bean wiring은 단순하지만, 변경 리뷰와 장애 분석 비용이 높다. 특히 outbox/consumer/alert/admin audit 기능이 계속 추가되면서 하나의 persistence adapter가 여러 bounded context의 SQL과 row mapper를 모두 소유하는 god-adapter가 됐다.

## 결정

Spring bean으로 노출되는 `JdbcWalletRepository`는 유지하되, SQL 구현은 bounded context별 package-private JDBC adapter로 분해한다.

| Context | Adapter | 책임 |
| --- | --- | --- |
| Wallet/Ledger | `JdbcWalletLedgerRepository` | member/account/balance 조회, charge/transfer command, transaction/ledger/audit/outbox 쓰기 |
| Outbox Relay | `JdbcOutboxRelayRepository` | pending/claim/published/failed/manual review/requeue/relay run |
| Outbox Consumer | `JdbcOutboxConsumerRepository` | idempotency, receipt, delivery metrics, monitoring, pruning |
| Operational Alert | `JdbcOperationalAlertRepository` | alert 저장, suppression lookup, 조회, pruning |
| Admin Audit | `JdbcAdminApiAccessAuditRepository` | admin access audit id 발급, 저장, 조회, pruning |
| Shared support | `WalletJdbcSupport` | `JdbcTemplate`, `TransactionTemplate`, mapper, timestamp/id/query helper |

`JdbcWalletRepository`는 기존 생성자와 구현 port 목록을 유지하고 각 method를 세부 adapter로 위임한다. 이렇게 해서 Spring wiring, `@Profile("postgres")`, 테스트의 생성 계약은 유지하면서 SQL 소유권만 나눈다.

## 대안

### 선택지 A: 기존 단일 `JdbcWalletRepository`를 유지한다

작업량은 가장 작지만, 기능이 늘어날수록 하나의 파일에서 여러 context의 SQL을 동시에 수정해야 한다. #116 평가에서 지적된 유지보수 문제를 해소하지 못한다.

### 선택지 B: 각 context adapter를 독립 Spring bean으로 등록한다

포트별 bean wiring이 더 명확해진다. 하지만 현재 서비스들은 여러 repository port를 같은 `JdbcWalletRepository` bean으로 주입받는 구조이고, profile별 bean 선택/테스트 생성자 변경 범위가 커진다.

### 선택지 C: composite bean 유지 + package-private 세부 adapter 분해

외부 wiring 안정성과 내부 책임 분리를 동시에 얻는다. 단점은 composite class가 여전히 많은 port interface를 구현한다는 점이다. 이 비용은 Spring bean 호환성을 지키기 위한 의도적 절충으로 둔다.

## 결과

- `JdbcWalletRepository`는 SQL을 직접 소유하지 않는 composite/delegator가 된다.
- context별 SQL은 작은 adapter 파일에 모여 변경 리뷰 범위가 좁아진다.
- `WalletJdbcSupport`는 mapper/helper 중복을 줄이되 외부에 공개하지 않는다.
- 기존 JDBC repository tests와 rollback tests는 같은 public adapter 계약을 계속 검증한다.
- ArchUnit 테스트로 domain/application/api/infra 핵심 의존 방향을 고정한다.

## 후속 작업

- 세부 adapter를 독립 Spring bean으로 승격할 필요가 생기면 별도 ADR에서 profile wiring과 bean 충돌 정책을 결정한다.
- `JdbcWalletRepositoryTest`가 너무 커지면 context별 repository slice test로 나누는 후속 작업을 검토한다.
