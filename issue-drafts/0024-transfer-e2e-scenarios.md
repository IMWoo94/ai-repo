# [Feature] 송금 성공/실패 E2E 시나리오 추가

GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/51

## 배경

현재 프론트 E2E는 초기 잔액 조회, 충전, operation 증거 확인을 검증한다. 하지만 사용자 화면의 핵심 기능인 송금 성공 흐름과 잔액 부족 실패 흐름은 아직 브라우저 기준으로 검증하지 않는다.

## 목표

- Playwright E2E에 송금 성공 시나리오를 추가한다.
- Playwright E2E에 잔액 부족 실패 시나리오를 추가한다.
- 테스트 가이드와 진행 문서에 현재 E2E 범위를 갱신한다.

## 범위

- `frontend/e2e/wallet-flow.spec.ts`
- `docs/testing/frontend-e2e-test-strategy.md`
- `docs/testing/local-test-guide.md`
- progress report

## 범위 제외

- 신규 화면 추가
- API 구현 변경
- manual review 운영자 화면 E2E
- 테스트 데이터 reset 전략 변경

## 수용 기준

- [ ] 송금 성공 후 출금 지갑 잔액이 감소한다.
- [ ] 송금 성공 후 최근 operation이 TRANSFER/COMPLETED로 표시된다.
- [ ] 송금 성공 후 step log와 TRANSFER_COMPLETED outbox event가 표시된다.
- [ ] 잔액 부족 송금 시 INSUFFICIENT_BALANCE 오류 메시지가 표시된다.
- [ ] `npm --prefix frontend run e2e`가 통과한다.
