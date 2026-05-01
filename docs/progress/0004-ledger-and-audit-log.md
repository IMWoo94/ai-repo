# 0004. 원장과 감사 로그

## 스펙 목표

- 돈 이동 결과는 사용자 거래내역과 별도로 원장 기록을 남긴다.
- 운영자/감사 관점에서 확인할 수 있는 감사 로그를 남긴다.
- 원장과 감사 로그는 이후 정합성 검증과 장애 추적의 기반이 되어야 한다.

## 완료 결과

- `LedgerEntry`와 `AuditEvent` 도메인 모델을 추가했다.
- 지갑별 원장 조회 API를 추가했다.
- 전체 감사 이벤트 조회 API를 추가했다.
- 충전/송금 성공 시 원장과 감사 로그가 함께 기록되도록 했다.

## 검증

- 도메인 테스트로 원장/감사 이벤트 필수값을 검증했다.
- 컨트롤러 테스트와 서비스 테스트로 조회 흐름을 검증했다.

## 남은 일

- 원장의 불변성 강제 수준은 더 강화할 수 있다.
- 관리자 권한, 감사 로그 검색 조건, 보존 기간 정책은 후속 작업이다.

## 관련 문서

- `docs/adr/0007-ledger-audit-log-boundary.md`
- `issue-drafts/0004-ledger-and-audit-log.md`
- `src/main/java/com/imwoo/airepo/wallet/api/WalletLedgerController.java`
