# ADR-0041: Requeue State Transition Atomicity

## 상태

Accepted

## 배경

Requeue workflow는 `REQUESTED -> APPROVED -> EXECUTED` 또는 `REQUESTED -> REJECTED`로 이동한다.

기존 JDBC 구현은 상태 조건이 있는 `update`를 사용했지만 update count를 확인하지 않았다. 동시에 approve/reject/execute가 호출되면 늦은 호출이 상태 변경 실패를 감지하지 못하거나, execute 경합에서 감사 이력이 중복될 수 있다.

## 결정

JDBC requeue 상태 전이는 request row와 대상 outbox event row를 `for update`로 잠근 뒤 조건부 update 결과를 검증한다.

- approve/reject는 request row를 잠그고 `REQUESTED` 상태에서만 1건 update되어야 성공한다.
- execute는 request row를 잠그고 `APPROVED` 상태를 확인한 뒤, outbox event row를 잠그고 `MANUAL_REVIEW -> PENDING` 전이가 1건 update되어야 audit을 기록한다.
- update count가 1이 아니면 `InvalidWalletOperationException`으로 실패시킨다.
- PostgreSQL Testcontainers에서 approve/reject 경합과 execute 경합을 검증한다.

## 트레이드오프

### 장점

- requeue workflow의 상태 전이가 DB 트랜잭션 안에서 직렬화된다.
- execute 경합에서 requeue audit 중복 기록을 방지한다.
- MSA 전환 전에도 DB 경계에서 상태 전이 불변식을 명확히 검증할 수 있다.

### 비용

- requeue request/event row에 짧은 row lock이 생긴다.
- JDBC 구현이 인메모리 구현보다 명시적인 동시성 제어 코드를 더 가진다.
- H2와 PostgreSQL 양쪽에서 동작 가능한 SQL 표현을 유지해야 한다.

## 검증 기준

- 같은 request에 execute가 동시에 들어와도 하나만 성공한다.
- execute 경합 후 requeue audit은 1건만 남는다.
- approve와 reject가 동시에 들어와도 하나의 전이만 성공한다.
- 경합 실패 호출은 `InvalidWalletOperationException`으로 실패한다.
- PostgreSQL Testcontainers gate에서 위 조건을 검증한다.

## 후속 작업

- Outbox publish `markPublished`/`markFailed`에도 claim token 또는 lease 조건을 추가한다.
- workflow 단계별 operator identity를 실제 인증 주체와 연결한다.
