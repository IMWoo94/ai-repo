# ADR-0023: Validation Summary Hardening

## 상태

Accepted

## 맥락

`_workspace/00_validation_summary.md`는 v0.6.0 기준선 검증에서 다음 개선 후보를 제시했다.

- 원장 조회가 잔액/거래내역 조회와 달리 지갑/회원 활성 상태를 확인하지 않는다.
- 실패 요청이 step log와 outbox event를 남기지 않는 정책은 코드상 성립하지만 InMemory 회귀 테스트가 부족하다.
- 미존재 operation의 step log/outbox 조회가 빈 목록으로 응답되어 오타나 잘못된 식별자를 정상 빈 결과처럼 위장한다.
- 일부 probe 테스트가 정식 테스트 위치로 이전되지 않았다.

## 선택지

### 선택지 A: 검증 보고서만 보관하고 코드는 변경하지 않는다

장점:

- 변경 범위가 없다.
- 정책 논의를 뒤로 미룰 수 있다.

단점:

- 확인된 결함이 계속 남는다.
- 검증 요약과 코드 기준선이 어긋난다.

### 선택지 B: 확정 결함과 회귀 보호만 즉시 반영한다

장점:

- 원장 조회 접근 정책을 잔액/거래내역과 일치시킨다.
- 실패 정책과 트랜잭션 원자성을 정식 테스트로 보호한다.
- 미존재 operation을 명시적으로 404 처리해 운영 분석 오인을 줄인다.

단점:

- operation 조회 API의 기존 빈 목록 동작이 변경된다.
- 공용 접근 정책 helper와 operation 존재 확인 repository 메서드가 추가된다.

### 선택지 C: 모든 probe와 Wiki 부채를 한 PR에서 처리한다

장점:

- 검증 보고서의 모든 항목을 한 번에 정리한다.

단점:

- 코드 수정, 정책 변경, Wiki 동기화가 섞여 리뷰 범위가 커진다.
- Testcontainers 강제 실행 같은 CI 정책은 별도 결정이 필요하다.

## 결정

`확정 결함과 회귀 보호만 즉시 반영한다`를 선택한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 원장 조회 접근 정책 | `WalletAccessPolicy.findQueryableWallet` 공용 helper 적용 |
| 비활성 지갑/회원 | 원장 조회도 `WalletAccountNotQueryableException` |
| 미존재 operation | step log/outbox 조회 모두 `OperationNotFoundException` |
| API 오류 | `404 NOT_FOUND`, `OPERATION_NOT_FOUND` |
| 실패 정책 회귀 | InMemory 실패 요청의 step log/outbox 미생성 테스트 |
| 트랜잭션 원자성 | JDBC charge 중 operation insert 실패 시 전체 rollback 테스트 |
| 범위 제외 | GitHub Wiki 실제 동기화, Testcontainers 강제 실행, broker adapter |

## 반박과 보류

- `미존재 operation은 빈 목록으로 둘 수 있다`는 반박은 채택하지 않는다. 현재 성공 operation은 step log와 outbox event를 남기는 정책이므로 빈 목록은 식별자 오류를 숨길 가능성이 더 크다.
- `fixtures-schema drift probe`는 이번 PR에서 보류한다. Flyway V2 seed migration이 이미 기준 경로이고, H2 fixture는 빠른 테스트 보조 경로라 rollback 회귀 보호보다 우선순위가 낮다.
- `CI Testcontainers SKIP 위험`은 별도 ADR이 필요하다. Docker availability를 강제하면 CI 비용과 실패 모드가 바뀌므로 v0.7.0 인프라 정책에서 결정한다.

## 결과

장점:

- 원장 조회와 잔액/거래내역 조회의 접근 정책이 일관된다.
- 실패 요청과 멱등키 충돌이 관측 로그를 오염시키지 않는다는 회귀 보호가 생긴다.
- operation 조회 API가 잘못된 식별자를 더 명확히 드러낸다.

비용:

- `GET /api/v1/operations/{operationId}/step-logs`와 `/outbox-events`는 미존재 operation에 대해 기존 `[]` 대신 404를 반환한다.
- 저장소 인터페이스에 `existsOperationId`가 추가된다.

후속 작업:

- Wiki 동기화 부채를 별도 작업으로 처리한다.
- Testcontainers 강제 실행 정책을 v0.7.0 CI ADR에서 결정한다.
- broker adapter 도입 시 operation/outbox 조회 정책을 다시 검토한다.
