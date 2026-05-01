# 0011. Transactional Outbox

## 스펙 목표

- 돈 이동 결과와 외부 이벤트 후보를 같은 트랜잭션에 저장한다.
- 외부 브로커 발행은 아직 하지 않고, outbox event 저장 경계부터 만든다.
- 이후 MSA 전환 시 서비스 간 정합성을 다루기 위한 기반을 만든다.

## 완료 결과

- `OperationOutboxEvent` 도메인 모델을 추가했다.
- 충전/송금 성공 시 `PENDING` outbox event를 저장하도록 했다.
- `GET /api/v1/operations/{operationId}/outbox-events` API를 추가했다.
- 실패한 요청은 outbox event를 만들지 않도록 정책을 정했다.

## 검증

- 도메인 테스트로 outbox event 필수값을 검증했다.
- 서비스 테스트로 성공 operation에서 outbox event가 생성되는지 확인했다.
- repository 테스트로 outbox event 저장과 조회를 검증했다.

## 남은 일

- 실제 메시지 브로커 발행은 아직 없다.
- event schema versioning과 consumer idempotency 정책이 필요하다.
- 다중 relay가 동시에 event를 가져가는 claiming 정책은 후속 작업이다.

## 관련 문서

- `docs/adr/0014-transactional-outbox-boundary.md`
- `issue-drafts/0011-transactional-outbox-boundary.md`
- `src/main/java/com/imwoo/airepo/wallet/domain/OperationOutboxEvent.java`
