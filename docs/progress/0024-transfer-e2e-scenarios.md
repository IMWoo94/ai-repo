# 0024. Transfer E2E Scenarios

## 스펙 목표

- React 사용자 화면 E2E에 송금 성공 흐름을 추가한다.
- 잔액 부족 송금 실패 흐름을 브라우저 기준으로 검증한다.
- 테스트 가이드의 현재 E2E 범위를 갱신한다.

## 완료 결과

- 기존 지갑 조회/충전 E2E를 송금 성공까지 확장했다.
- 송금 성공 후 출금 지갑 잔액 감소, `TRANSFER` operation, `TRANSFER_COMPLETED` outbox event를 검증한다.
- 잔액 부족 송금 시 `INSUFFICIENT_BALANCE` 오류 메시지를 검증하는 E2E를 추가했다.
- `frontend-e2e-test-strategy.md`와 `local-test-guide.md`의 E2E 범위를 갱신했다.

## 검증

- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `./gradlew test scenarioTest check`

## 남은 일

- 운영자 manual review 화면이 생기면 outbox requeue E2E를 추가한다.
- 반복 실행 안정성이 떨어지면 테스트 데이터 reset 전략을 별도 ADR로 정리한다.

## 관련 문서

- `frontend/e2e/wallet-flow.spec.ts`
- `docs/testing/frontend-e2e-test-strategy.md`
- `docs/testing/local-test-guide.md`
- `issue-drafts/0024-transfer-e2e-scenarios.md`
