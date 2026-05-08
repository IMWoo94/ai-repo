# Requeue State Transition Atomicity

## 배경

Requeue workflow는 금융 운영 조치이므로 동시 호출 상황에서도 한 번의 승인, 반려, 실행만 성공해야 한다.

기존 JDBC 구현은 상태 조건 update를 사용했지만 update count를 검증하지 않아 경합 상황에서 실패 전이를 명확히 감지하기 어렵다.

## 목표

- approve/reject/execute 상태 전이를 DB 트랜잭션 안에서 원자화한다.
- execute 경합에서 audit 중복 생성을 방지한다.
- PostgreSQL Testcontainers로 실제 DB 동시성 시나리오를 검증한다.

## 완료 기준

- [x] requeue request row lock을 사용한다.
- [x] execute 시 outbox event row lock을 사용한다.
- [x] 조건부 update count가 1이 아니면 실패한다.
- [x] concurrent execute는 하나만 성공하고 audit은 1건만 남는다.
- [x] concurrent approve/reject는 하나만 성공한다.
- [x] PostgreSQL scenario gate에서 검증한다.

## 제외

- outbox publish result lease owner 조건
- 실제 로그인/OIDC role scope 연결
