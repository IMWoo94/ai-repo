# 0003. 충전과 송금

## 스펙 목표

- 지갑에 금액을 충전할 수 있다.
- 다른 지갑으로 송금할 수 있다.
- 같은 멱등키로 같은 요청을 반복하면 잔액은 한 번만 변경된다.
- 충전/송금은 `KRW` 단일 통화와 양수 금액만 허용한다.

## 완료 결과

- `POST /api/v1/wallets/{walletId}/charges` API를 추가했다.
- `POST /api/v1/wallets/{walletId}/transfers` API를 추가했다.
- 멱등키 기반으로 신규 요청은 `201 Created`, 동일 재시도는 `200 OK`를 반환하도록 했다.
- 잔액 부족, 잘못된 금액, 통화 오류, 멱등키 충돌 예외를 정의했다.

## 검증

- 컨트롤러 테스트로 충전/송금 응답 상태와 오류 응답을 검증했다.
- 서비스 테스트로 잔액 변경, 잔액 부족, 멱등 재시도, 멱등키 충돌을 검증했다.

## 남은 일

- 외부 결제망 충전 승인 흐름은 아직 범위 밖이다.
- 일/월 한도, 이상거래 탐지, 취소/환불 정책은 후속 작업이다.

## 관련 문서

- `docs/adr/0006-charge-transfer-idempotency-policy.md`
- `issue-drafts/0003-charge-and-transfer.md`
- `src/main/java/com/imwoo/airepo/wallet/api/WalletCommandController.java`
