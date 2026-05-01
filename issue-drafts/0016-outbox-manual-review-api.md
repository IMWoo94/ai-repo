# Outbox Manual Review API

## 배경

Outbox event는 3회 실패하면 `MANUAL_REVIEW` 상태로 격리된다. 하지만 현재는 운영자가 어떤 event가 격리되었는지 조회하거나, 원인 조치 후 다시 처리 흐름으로 넣을 API가 없다.

## 목표

- `MANUAL_REVIEW` outbox event를 제한 개수만큼 조회한다.
- 운영자가 manual review event를 다시 `PENDING`으로 requeue할 수 있게 한다.
- requeue 시 retry/lease/publish/error 메타데이터를 초기화한다.
- 코드, 테스트, ADR, Wiki, Progress Report를 함께 갱신한다.

## 완료 조건

- [ ] `MANUAL_REVIEW` event 조회 API가 있다.
- [ ] manual review event requeue API가 있다.
- [ ] requeue된 event는 다시 claim 대상이 된다.
- [ ] `MANUAL_REVIEW`가 아닌 event requeue 정책이 명확하다.
- [ ] in-memory와 JDBC 구현 모두 테스트가 있다.
- [ ] API 테스트가 있다.
- [ ] ADR/Wiki/Progress 문서가 갱신된다.

## 범위 제외

- 인증/인가
- 운영자 승인 이력 테이블
- 알림/모니터링 연동
- 실제 broker DLQ replay

## GitHub Issue

- https://github.com/IMWoo94/ai-repo/issues/33
