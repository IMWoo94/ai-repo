# JDBC Persistence Adapter Decomposition Codex Review

## 검토 대상

- 범위: Issue #125 `JdbcWalletRepository` bounded-context 분해와 ArchUnit 레이어 규칙
- 변경 의도: PostgreSQL profile의 기존 composite bean 계약은 유지하면서 SQL 구현을 wallet/ledger, outbox relay, outbox consumer, operational alert, admin audit adapter로 분리하고, application annotation 정책과 persistence 분해 전략을 ADR로 고정한다.

## 검증

리뷰 과정에서 다음을 확인했다.

```bash
./gradlew test --tests '*LayerDependencyTest'
./gradlew postgresScenarioTest
./gradlew test scenarioTest postgresScenarioTest
scripts/check-dev-rules.sh
git diff --check
```

## 판정

통과. 블로커 없음.

## 백엔드 구조

- `JdbcWalletRepository`가 기존 Spring bean과 public constructor를 유지하고 내부 adapter에 위임하므로 service wiring과 기존 repository tests의 호출 계약이 유지된다.
- context별 adapter는 package-private으로 남아 외부 API를 늘리지 않는다.
- `WalletJdbcSupport`가 mapper/helper를 공유해 분해 과정에서 SQL row mapping 중복을 과하게 늘리지 않는다.
- composite가 여전히 여러 port를 구현하는 비용은 ADR-0064에서 명시한 호환성 절충으로 설명되어 있다.

## 아키텍처 규칙

- ArchUnit 테스트는 production class만 분석하도록 `DoNotIncludeTests`를 사용한다.
- 규칙은 domain 순수성, application의 adapter 비의존, api의 infra/config 비의존, infra의 api 비의존을 확인한다.
- application layer의 `@Service`, policy/properties `@Component`/`@Value`, 필요한 `@Transactional` 허용은 ADR-0063과 일치한다.

## 테스트

- `./gradlew test scenarioTest postgresScenarioTest`가 통과했다.
- PostgreSQL scenario는 `postgres` profile의 기본 JWT/운영 토큰 fail-fast 정책 때문에 테스트용 secret/token override가 필요했고, `DynamicPropertySource`에 명시 값을 추가해 통과시켰다.
- 기존 JDBC repository/rollback 계약은 전체 unit test와 PostgreSQL scenario로 함께 검증된다.

## 문서

- ADR-0063/0064, progress 0076, unreleased release note, issue draft, Wiki draft가 함께 갱신되어 dev rule sync를 통과한다.
- 기존 ADR index가 0057에서 멈춰 있던 drift도 0058-0064까지 보강했다.

## 리뷰 반영

### 수용

- PostgreSQL scenario가 deployed profile fail-fast 정책을 통과하도록 테스트용 JWT/admin/operator token property를 명시했다.
- ArchUnit 분석 대상에서 test classes를 제외하고, package matcher를 project package로 한정해 Spring `security.config` 패키지 오탐을 제거했다.
- dev rules가 요구한 issue draft와 Wiki draft sync를 추가했다.

### 반박

- 없음.

### 후속 과제

- 세부 JDBC adapter별 slice test 분리가 필요하면 별도 이슈로 다룬다.
- composite bean을 제거하고 각 context adapter를 독립 Spring bean으로 승격할지는 별도 ADR에서 결정한다.
