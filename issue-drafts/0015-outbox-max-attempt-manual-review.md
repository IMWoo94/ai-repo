# Outbox Max Attempt and Manual Review

## 배경

Outbox retry schedule과 processing lease recovery가 추가되었지만, 계속 실패하는 event는 무한히 재시도될 수 있다. 실제 금융/핀테크 운영에서는 반복 실패 event를 자동 재시도에서 분리해 운영자가 확인할 수 있는 상태로 격리해야 한다.

## 목표

- outbox event에 최대 재시도 횟수 정책을 추가한다.
- 최대 재시도에 도달한 event는 자동 claim 대상에서 제외한다.
- 최종 실패 event는 `MANUAL_REVIEW` 상태로 전이한다.
- manual review 상태의 오류와 attempt count를 조회 가능하게 유지한다.
- ADR, Wiki, Progress Report에 결정과 한계를 기록한다.

## 완료 조건

- [ ] outbox status에 `MANUAL_REVIEW` 또는 동등한 최종 격리 상태가 있다.
- [ ] 실패 처리 시 다음 attempt가 max attempt 이상이면 `MANUAL_REVIEW`로 전이한다.
- [ ] `MANUAL_REVIEW` event는 claim 대상이 아니다.
- [ ] `FAILED` event만 retry schedule 기준으로 다시 claim된다.
- [ ] in-memory와 JDBC 구현 모두 테스트가 있다.
- [ ] ADR/Wiki/Progress 문서가 갱신된다.

## 범위 제외

- 운영자 수동 재처리 API
- 실제 DLQ broker topic/queue
- 알림/모니터링 연동

## GitHub Issue

- https://github.com/IMWoo94/ai-repo/issues/31
