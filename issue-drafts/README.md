# Issue Drafts

이 폴더는 GitHub Issue로 옮기기 전 작업 초안을 보관합니다.

초안은 `.github/ISSUE_TEMPLATE/feature.yml` 또는 `.github/ISSUE_TEMPLATE/bug.yml`에 맞춰 작성합니다. GitHub Issue를 생성한 뒤에는 실제 Issue 링크를 PR과 Wiki에 연결합니다.

## 목록

- `0001-balance-and-transaction-history.md`: 첫 기능 흐름인 잔액/거래내역 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/1
- `0002-member-wallet-account.md`: 회원/지갑 계정 모델 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/3
- `0003-charge-and-transfer.md`: 충전/송금 1차 흐름 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/5
- `0004-ledger-and-audit-log.md`: 원장/감사 로그 1차 모델과 조회 API 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/7
- `0005-postgresql-persistence.md`: PostgreSQL 저장소와 멱등키/원장 영속화 1차 도입 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/9
- `0006-postgresql-runtime-verification.md`: PostgreSQL 런타임 검증과 로컬 DB 실행 환경 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/11
- `0007-flyway-schema-migrations.md`: Flyway 기반 스키마 버전 관리 도입 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/13
- `0008-postgresql-transfer-concurrency-lock.md`: PostgreSQL 송금 동시성 잠금 정책 검증 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/15
- `0009-postgresql-lock-timeout-policy.md`: PostgreSQL lock timeout과 busy wallet 오류 정책 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/17
- `0010-operation-step-log.md`: 논리적 트랜잭션 단계 로그 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/19
- `0011-transactional-outbox-boundary.md`: Transactional Outbox 경계 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/21
- `0012-outbox-relay-state.md`: Outbox relay 상태 전이와 재시도 메타데이터 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/23
- `0013-outbox-claiming-retry.md`: Outbox claiming과 retry schedule 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/27
- `0014-outbox-processing-lease-recovery.md`: Outbox processing lease recovery 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/29
- `0015-outbox-max-attempt-manual-review.md`: Outbox max attempt와 manual review 격리 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/31
- `0016-outbox-manual-review-api.md`: Outbox manual review 조회와 requeue API 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/33
- `0017-outbox-requeue-audit-trail.md`: Outbox requeue 감사 이력 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/35
- `0018-release-version-baseline.md`: v0.6.0 첫 검증 기준선 발행 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/37
- `0019-scenario-test-pipeline.md`: 시나리오 기반 테스트 파이프라인 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/39
- `0020-validation-summary-hardening.md`: 검증 요약 기반 코드 하드닝 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/41
- `0021-react-user-frontend-mvp.md`: React 사용자 화면 프론트 MVP 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/43
- `0022-frontend-e2e-test-pipeline.md`: React 사용자 화면 E2E 테스트 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/47
- `0023-local-test-guide.md`: 백엔드/프론트 테스트 가이드 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/49
- `0024-transfer-e2e-scenarios.md`: 송금 성공/실패 E2E 시나리오 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/51
- `0025-frontend-component-tests.md`: 프론트 컴포넌트 테스트 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/53
- `0026-outbox-publisher-port.md`: Outbox publisher port 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/55
- `0027-admin-api-authz.md`: 운영 API 인증/인가 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/57
- `0028-outbox-relay-scheduler.md`: Outbox relay scheduler 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/59
- `0029-outbox-relay-run-monitoring.md`: Outbox relay 실행 기록/모니터링 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/61
- `0030-admin-api-access-audit.md`: 운영 API 접근 감사 로그 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/63
- `0031-operational-log-pruning.md`: 운영 로그 보존 기간과 pruning 정책 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/65
- `0032-outbox-relay-health-metrics-alert.md`: Outbox relay health metric과 alert 판정 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/67
- `0033-spring-security-role-model.md`: Spring Security 기반 운영 API role model 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/69
- `0034-http-outbox-broker-adapter.md`: HTTP outbox broker adapter와 contract test 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/71

## GitHub CLI로 생성

GitHub CLI가 설치되어 있고 인증되어 있다면 다음 명령으로 첫 Issue를 생성할 수 있습니다.

```bash
gh issue create \
  --repo IMWoo94/ai-repo \
  --title "[Feature] 잔액 조회와 거래내역 조회 기준 정의" \
  --label feature \
  --body-file issue-drafts/0001-balance-and-transaction-history.md
```
