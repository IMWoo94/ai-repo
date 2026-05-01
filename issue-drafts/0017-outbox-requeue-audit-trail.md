# Outbox Requeue Audit Trail

## 배경

Manual review outbox event를 `PENDING`으로 requeue하는 API가 추가되었지만, 누가/왜/언제 requeue 했는지 남기는 감사 이력이 없다. 금융/핀테크 운영에서는 재처리 행위 자체도 감사 대상이다.

## 목표

- manual review requeue 요청 시 감사 이력을 남긴다.
- requeue 요청에는 operator와 reason을 받는다.
- requeue 이력을 조회할 수 있는 API를 추가한다.
- in-memory와 JDBC 구현 모두 같은 정책을 따른다.
- ADR, Wiki, Progress Report에 결정과 한계를 기록한다.

## 완료 조건

- [ ] requeue 요청 body에 operator, reason이 있다.
- [ ] requeue 성공 시 감사 이력이 저장된다.
- [ ] operation/outbox event 기준 requeue 이력을 조회할 수 있다.
- [ ] operator/reason 검증이 있다.
- [ ] in-memory와 JDBC 구현 모두 테스트가 있다.
- [ ] API 테스트가 있다.
- [ ] ADR/Wiki/Progress 문서가 갱신된다.

## 범위 제외

- 실제 인증/인가와 로그인 사용자 연동
- 승인 워크플로우
- 알림/모니터링 연동

## GitHub Issue

- https://github.com/IMWoo94/ai-repo/issues/35
