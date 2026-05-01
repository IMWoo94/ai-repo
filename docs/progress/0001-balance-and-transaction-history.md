# 0001. 잔액과 거래내역 조회

## 스펙 목표

- 첫 기능은 잔액 조회와 거래내역 조회로 시작한다.
- 존재하지 않는 지갑은 실패로 처리하고, 거래내역이 없으면 빈 목록을 정상 응답으로 반환한다.
- 잔액은 금액과 통화를 함께 가진다.

## 완료 결과

- `GET /api/v1/wallets/{walletId}/balance` API를 추가했다.
- `GET /api/v1/wallets/{walletId}/transactions` API를 추가했다.
- 지갑 잔액, 거래내역, 거래 방향, 거래 상태 도메인 모델을 구성했다.
- API 예외 응답 형식을 추가했다.

## 검증

- 컨트롤러 테스트로 정상 잔액 조회, 거래내역 조회, 존재하지 않는 지갑 실패를 검증했다.
- 애플리케이션 서비스 테스트로 조회 정책을 검증했다.

## 남은 일

- 페이지네이션과 정렬 파라미터 정책은 아직 확정하지 않았다.
- 인증/인가 기반의 지갑 접근 권한은 아직 범위 밖이다.

## 관련 문서

- `issue-drafts/0001-balance-and-transaction-history.md`
- `wiki-drafts/Domain-Rules.md`
- `src/main/java/com/imwoo/airepo/wallet/api/WalletQueryController.java`
