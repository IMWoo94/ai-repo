# 0002. 회원과 지갑 계정

## 스펙 목표

- 지갑은 소유 회원 식별자를 가진다.
- 초기 조회 정책은 활성 회원의 활성 지갑만 허용한다.
- 비활성 회원/지갑의 세부 보존 정책은 후속 결정으로 남긴다.

## 완료 결과

- `Member`, `WalletAccount`, `MemberStatus`, `WalletAccountStatus` 도메인 모델을 추가했다.
- 조회 서비스가 활성 회원과 활성 지갑 조건을 확인하도록 확장했다.
- 비활성 계정 조회 실패 예외를 추가했다.
- 회원/지갑 계정 조회 정책을 ADR로 확정했다.

## 검증

- 도메인 테스트로 회원과 지갑 계정의 필수값과 상태를 검증했다.
- 서비스 테스트로 비활성 회원/지갑 조회 실패를 검증했다.

## 남은 일

- 회원 가입, 지갑 생성 API는 아직 구현하지 않았다.
- `SUSPENDED`, `CLOSED` 상태의 상세 열람/보존 정책은 후속 결정이 필요하다.

## 관련 문서

- `docs/adr/0005-member-wallet-account-query-policy.md`
- `issue-drafts/0002-member-wallet-account.md`
- `wiki-drafts/Domain-Rules.md`
