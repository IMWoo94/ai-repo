# 0020. Validation Summary Hardening

## 스펙 목표

- `_workspace/00_validation_summary.md`의 확정 결함 중 즉시 수정 가치가 큰 항목을 반영한다.
- 원장 조회 접근 정책을 잔액/거래내역 조회와 일치시킨다.
- 실패 정책, operation 조회 정책, JDBC rollback을 정식 테스트로 보호한다.

## 완료 결과

- `WalletAccessPolicy`를 추가해 queryable 지갑/회원 활성 상태 검증을 공용화했다.
- 원장 조회에도 SUSPENDED/CLOSED 지갑과 비활성 회원 소유 지갑 차단을 적용했다.
- 미존재 operation의 step log/outbox 조회를 `OperationNotFoundException`으로 변경했다.
- API 오류 코드는 `OPERATION_NOT_FOUND`로 분리했다.
- InMemory 실패 요청의 step log/outbox 미생성 테스트를 추가했다.
- JDBC charge 트랜잭션 rollback 테스트를 추가했다.
- ADR-0023에 반영/보류/반박 근거를 기록했다.

## 검증

- `InMemoryWalletLedgerQueryServiceTest`로 원장 조회 접근 정책과 미존재 operation 정책을 검증한다.
- `WalletLedgerControllerTest`로 미존재 operation API가 404를 반환하는지 검증한다.
- `StepLogFailurePolicyTest`로 실패 요청이 step log/outbox를 남기지 않는지 검증한다.
- `JdbcWalletRepositoryRollbackTest`로 operation insert 실패 시 charge 트랜잭션 전체 rollback을 검증한다.
- `WalletApiExceptionHandlerTest`로 `OPERATION_NOT_FOUND` API 오류 매핑을 검증한다.
- `./gradlew test scenarioTest check`로 전체 회귀를 확인한다.

## 남은 일

- GitHub Wiki 실제 동기화는 아직 남아 있다.
- Testcontainers 강제 실행 정책은 별도 CI ADR에서 결정한다.
- fixtures-schema drift probe는 Flyway seed 기준 정리 이후 별도 판단한다.

## 관련 문서

- `docs/adr/0023-validation-summary-hardening.md`
- `issue-drafts/0020-validation-summary-hardening.md`
- `_workspace/00_validation_summary.md`
