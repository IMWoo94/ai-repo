# Outbox Processing Lease Recovery

## 배경

Outbox claiming과 retry schedule이 추가되었지만, relay worker가 event를 `PROCESSING`으로 claim한 뒤 crash 되면 해당 event가 영구적으로 처리 중 상태에 머무를 수 있다.

## 목표

- `PROCESSING` event에 lease 시각을 기록한다.
- lease가 만료된 `PROCESSING` event는 다시 claim 대상이 되게 한다.
- claim 시 lease 만료 시각을 갱신한다.
- 발행 성공/실패 시 lease 관련 필드는 초기화한다.
- ADR, Wiki, Progress Report에 결정과 한계를 기록한다.

## 완료 조건

- [ ] outbox event에 `claimedAt`, `leaseExpiresAt` 또는 동등한 lease 메타데이터가 있다.
- [ ] claim된 event는 `PROCESSING`과 lease 만료 시각을 가진다.
- [ ] lease가 만료되지 않은 `PROCESSING` event는 claim 대상이 아니다.
- [ ] lease가 만료된 `PROCESSING` event는 다시 claim 가능하다.
- [ ] 발행 성공/실패 시 lease 메타데이터가 초기화된다.
- [ ] in-memory와 JDBC 구현 모두 테스트가 있다.
- [ ] ADR/Wiki/Progress 문서가 갱신된다.

## 범위 제외

- 실제 broker adapter 구현
- scheduler/poller 자동 실행
- max attempt, DLQ, manual review 정책

## GitHub Issue

- https://github.com/IMWoo94/ai-repo/issues/29
