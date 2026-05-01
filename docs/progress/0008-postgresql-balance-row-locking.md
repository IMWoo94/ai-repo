# 0008. PostgreSQL 잔액 행 락

## 스펙 목표

- 동시에 송금이 들어와도 같은 지갑 잔액이 꼬이지 않아야 한다.
- PostgreSQL에서는 잔액 변경 대상 지갑 행을 잠그고 작업한다.
- 모놀리식 상태에서도 이후 MSA 전환을 고려해 정합성 경계를 명확히 한다.

## 완료 결과

- PostgreSQL repository에서 잔액 변경 시 row-level lock을 사용하도록 했다.
- 송금 시 출금 지갑과 입금 지갑의 잠금 순서를 안정적으로 처리했다.
- 동시성 테스트를 통해 잔액 정합성 위험을 검증했다.

## 검증

- repository 테스트로 PostgreSQL 잔액 변경 경로를 검증했다.
- 동시 송금 상황에서 잔액이 음수가 되거나 중복 차감되지 않도록 확인했다.

## 남은 일

- 다중 DB 또는 분산 트랜잭션 상황은 아직 범위 밖이다.
- 장기적으로 saga, outbox, 보상 트랜잭션과 연결해야 한다.

## 관련 문서

- `docs/adr/0011-postgresql-balance-row-locking.md`
- `issue-drafts/0008-postgresql-transfer-concurrency-lock.md`
- `src/main/java/com/imwoo/airepo/wallet/infra/JdbcWalletRepository.java`
