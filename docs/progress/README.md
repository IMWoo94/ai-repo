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

## 현재 기준선

- 최신 병합 기준: `main`
- 최신 완료 기능: Validation Summary Hardening
- 현재 릴리스 후보: `v0.6.0`
- 아직 미완료: 실제 broker 발행, 승인 워크플로우, PostgreSQL scenario test, GitHub Wiki 동기화, Testcontainers 강제 실행 정책
