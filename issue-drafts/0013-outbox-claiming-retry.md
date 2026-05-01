# Outbox Claiming and Retry

## 배경

Transactional Outbox는 생성과 relay 상태 관리까지 완료되었지만, 여러 relay worker가 동시에 pending event를 가져갈 때 중복 처리하지 않도록 claiming 경계가 필요하다.

현재 남은 리스크:

- pending event 단순 조회는 다중 relay 환경에서 같은 event를 동시에 가져갈 수 있다.
- failed event 재시도 정책이 없어 일시 장애 복구 흐름이 약하다.
- retry backoff 기준이 없어 실패 직후 무한 재시도 위험이 있다.

## 목표

- Outbox event에 claiming 상태를 추가한다.
- PostgreSQL에서는 `FOR UPDATE SKIP LOCKED` 기반으로 claim한다.
- 실패 event는 attempt count와 next retry time 기준으로 재시도 후보가 되게 한다.
- 이번 단계 결과를 ADR, Wiki, Progress Report에 남긴다.

## 완료 조건

- [ ] claim 가능한 outbox event를 제한 개수만큼 가져오는 repository/service API가 있다.
- [ ] claimed event는 `PROCESSING` 상태가 되어 다른 worker가 동시에 가져가지 못한다.
- [ ] 발행 실패 시 `FAILED`, `attemptCount + 1`, `lastError`, `nextRetryAt`이 기록된다.
- [ ] `PENDING` 또는 retry 가능한 `FAILED` event만 claim 대상이다.
- [ ] in-memory와 JDBC 구현 모두 테스트가 있다.
- [ ] ADR/Wiki/Progress 문서가 갱신된다.

## 범위 제외

- 실제 broker adapter 구현
- scheduler/poller 자동 실행
- DLQ 또는 영구 폐기 정책

## GitHub Issue

- https://github.com/IMWoo94/ai-repo/issues/27
