# JdbcWalletRepository bounded-context 분해 + ArchUnit 레이어 규칙

GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/125

## 배경

`JdbcWalletRepository`는 PostgreSQL profile에서 wallet command/query, ledger, outbox relay, consumer idempotency/monitoring/pruning, operational alert, admin access audit port를 한 클래스에서 구현했다. 기능이 누적되면서 SQL 변경 리뷰 범위가 커졌고, 레이어 의존 규칙도 문서 계약에 머물렀다.

## 목표

- application layer Spring annotation 정책을 ADR로 확정한다.
- JDBC persistence adapter 분해 전략을 ADR로 확정한다.
- `JdbcWalletRepository`를 context별 JDBC adapter로 분해하되 기존 Spring bean 계약은 유지한다.
- ArchUnit 테스트로 레이어 의존 방향을 고정한다.

## 완료 조건

- [x] ADR-0063: application layer Spring annotation policy
- [x] ADR-0064: JDBC persistence adapter decomposition
- [x] `JdbcWalletRepository` composite 유지 + context별 package-private adapter 분해
- [x] ArchUnit 레이어 의존 규칙 테스트 추가
- [x] PostgreSQL scenario가 배포 profile credential fail-fast 정책과 함께 통과하도록 test property 보정

## 검증

```bash
./gradlew test scenarioTest postgresScenarioTest
scripts/check-dev-rules.sh
git diff --check
```

## 관련 문서

- `docs/adr/0063-application-layer-spring-annotation-policy.md`
- `docs/adr/0064-jdbc-persistence-adapter-decomposition.md`
- `docs/progress/0076-jdbc-persistence-adapter-decomposition.md`
- `docs/releases/unreleased.md`
