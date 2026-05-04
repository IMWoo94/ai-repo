# Progress Reports

이 폴더는 단계별 작업 완료 흔적을 남긴다.

ADR은 “왜 그렇게 결정했는가”를 기록하고, 이 폴더는 “각 단계에서 무엇이 완료되었고 무엇이 남았는가”를 짧게 기록한다.

## 작성 기준

- 한 단계는 하나의 Markdown 파일로 남긴다.
- 각 파일은 스펙 목표, 완료 결과, 검증, 남은 일을 포함한다.
- 기능 구현이 없는 문서/운영 단계도 독립 단계로 기록한다.
- 릴리스 전에는 이 문서들을 근거로 릴리스 노트를 작성한다.

## 완료 단계

| 단계 | 문서 | 상태 |
| --- | --- | --- |
| 0000 | [하네스와 문서 기반](0000-harness-and-documentation-foundation.md) | 완료 |
| 0001 | [잔액과 거래내역 조회](0001-balance-and-transaction-history.md) | 완료 |
| 0002 | [회원과 지갑 계정](0002-member-wallet-account.md) | 완료 |
| 0003 | [충전과 송금](0003-charge-and-transfer.md) | 완료 |
| 0004 | [원장과 감사 로그](0004-ledger-and-audit-log.md) | 완료 |
| 0005 | [PostgreSQL 저장소 프로필](0005-postgresql-persistence-profile.md) | 완료 |
| 0006 | [PostgreSQL 런타임 검증](0006-postgresql-runtime-verification.md) | 완료 |
| 0007 | [Flyway 스키마 마이그레이션](0007-flyway-schema-migrations.md) | 완료 |
| 0008 | [PostgreSQL 잔액 행 락](0008-postgresql-balance-row-locking.md) | 완료 |
| 0009 | [PostgreSQL 락 타임아웃](0009-postgresql-lock-timeout-policy.md) | 완료 |
| 0010 | [Operation Step Log](0010-operation-step-log.md) | 완료 |
| 0011 | [Transactional Outbox](0011-transactional-outbox-boundary.md) | 완료 |
| 0012 | [Outbox Relay 상태](0012-outbox-relay-state.md) | 완료 |
| 0013 | [Outbox Claiming과 Retry](0013-outbox-claiming-retry.md) | 완료 |
| 0014 | [Outbox Processing Lease Recovery](0014-outbox-processing-lease-recovery.md) | 완료 |
| 0015 | [Outbox Max Attempt와 Manual Review](0015-outbox-max-attempt-manual-review.md) | 완료 |
| 0016 | [Outbox Manual Review API](0016-outbox-manual-review-api.md) | 완료 |
| 0017 | [Outbox Requeue Audit Trail](0017-outbox-requeue-audit-trail.md) | 완료 |
| 0018 | [Release Version Baseline](0018-release-version-baseline.md) | 완료 |
| 0019 | [Scenario Test Pipeline](0019-scenario-test-pipeline.md) | 완료 |
| 0020 | [Validation Summary Hardening](0020-validation-summary-hardening.md) | 완료 |
| 0021 | [React User Frontend MVP](0021-react-user-frontend-mvp.md) | 완료 |
| 0022 | [Frontend E2E Test Pipeline](0022-frontend-e2e-test-pipeline.md) | 완료 |
| 0023 | [Local Test Guide](0023-local-test-guide.md) | 완료 |
| 0024 | [Transfer E2E Scenarios](0024-transfer-e2e-scenarios.md) | 완료 |
| 0025 | [Frontend Component Tests](0025-frontend-component-tests.md) | 완료 |
| 0026 | [Outbox Publisher Port](0026-outbox-publisher-port.md) | 완료 |
| 0027 | [Admin API Authz](0027-admin-api-authz.md) | 완료 |
| 0028 | [Outbox Relay Scheduler](0028-outbox-relay-scheduler.md) | 완료 |
| 0029 | [Outbox Relay Run Monitoring](0029-outbox-relay-run-monitoring.md) | 완료 |
| 0030 | [Admin API Access Audit](0030-admin-api-access-audit.md) | 완료 |
| 0031 | [Operational Log Pruning](0031-operational-log-pruning.md) | 완료 |
| 0032 | [Outbox Relay Health Metrics and Alert](0032-outbox-relay-health-metrics-alert.md) | 완료 |
| 0033 | [Spring Security Role Model](0033-spring-security-role-model.md) | 완료 |
| 0034 | [HTTP Outbox Broker Adapter](0034-http-outbox-broker-adapter.md) | 완료 |
| 0035 | [PostgreSQL Scenario Testcontainers CI](0035-postgresql-scenario-testcontainers-ci.md) | 완료 |
| 0036 | [Operator Manual Review Console UI](0036-operator-manual-review-console-ui.md) | 완료 |
| 0037 | [Operator Console E2E Smoke](0037-operator-console-e2e-smoke.md) | 완료 |
| 0038 | [Unreleased Release Candidate Notes](0038-unreleased-release-candidate-notes.md) | 완료 |

## 현재 기준선

- 최신 병합 기준: `main`
- 최신 완료 기능: Unreleased Release Candidate Notes
- 현재 릴리스 후보: `unreleased`
- 아직 미완료: 운영자 relay health/pruning 화면, Kafka/RabbitMQ/SQS adapter, consumer idempotency, operator/admin token 분리, pruning 실행 이력, external alert channel, 승인 워크플로우, GitHub Wiki 동기화, broker-specific Testcontainers 정책, manual review requeue full E2E fixture
