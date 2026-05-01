# 0010. Operation Step Log

## 스펙 목표

- 금융/핀테크 흐름은 한 요청 안에서도 단계별 관측이 가능해야 한다.
- 충전/송금 처리 과정에서 어떤 단계가 실행되었는지 기록한다.
- 이후 outbox, saga, 장애 보상으로 확장하기 전 내부 처리 과정을 먼저 보이게 한다.

## 완료 결과

- `OperationStepLog` 도메인 모델을 추가했다.
- 충전/송금 처리 중 검증, 잔액 변경, 거래 기록, 원장 기록, 감사 기록 단계를 남기도록 했다.
- `GET /api/v1/operations/{operationId}/step-logs` API를 추가했다.

## 검증

- 도메인 테스트로 step log 필수값과 상태를 검증했다.
- 서비스 테스트로 충전/송금 성공 시 단계 로그가 남는지 확인했다.
- 컨트롤러 테스트로 operation step log 조회 API를 검증했다.

## 남은 일

- 단계별 소요 시간, correlation id, request id 연결은 아직 없다.
- 실패 단계 로그와 보상 단계 기록은 후속으로 확장해야 한다.

## 관련 문서

- `docs/adr/0013-operation-step-log-before-outbox-saga.md`
- `issue-drafts/0010-operation-step-log.md`
- `src/main/java/com/imwoo/airepo/wallet/domain/OperationStepLog.java`
