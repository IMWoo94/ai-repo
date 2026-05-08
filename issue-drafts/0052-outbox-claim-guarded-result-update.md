# Outbox Claim Guarded Result Update

## 배경

Outbox event는 lease 만료 후 재claim될 수 있다. 이때 이전 worker가 늦게 publish 결과를 기록하면 새 claim 상태를 덮어쓸 수 있다.

## 목표

- publish success/failure 결과 갱신에 claim 조건을 추가한다.
- stale writer가 재claim된 event를 변경하지 못하게 한다.
- H2와 PostgreSQL Testcontainers에서 방어 시나리오를 검증한다.

## 완료 기준

- [x] claim 기반 publish success update가 있다.
- [x] claim 기반 publish failure update가 있다.
- [x] publish loop는 guarded update를 사용한다.
- [x] stale success writer는 실패한다.
- [x] stale failure writer는 실패한다.
- [x] PostgreSQL Testcontainers에서 stale writer 방지 검증이 있다.

## 제외

- worker identity 컬럼 도입
- broker/consumer idempotency
