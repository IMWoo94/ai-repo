# 0009. PostgreSQL 락 타임아웃

## 스펙 목표

- 잔액 행 락 대기가 무한정 길어지지 않아야 한다.
- 락 대기 실패는 명확한 도메인 오류로 변환되어야 한다.
- 클라이언트와 운영자는 재시도 가능한 실패를 구분할 수 있어야 한다.

## 완료 결과

- PostgreSQL lock timeout 설정을 추가했다.
- DB 락 타임아웃을 `WALLET_BALANCE_BUSY` 오류로 변환했다.
- API 예외 처리에서 동시성 오류 응답을 제공하도록 확장했다.

## 검증

- repository 테스트로 lock timeout 상황을 검증했다.
- API 예외 핸들러 테스트로 오류 응답 매핑을 검증했다.

## 남은 일

- 클라이언트 재시도 backoff 정책은 아직 정의하지 않았다.
- 운영 모니터링 지표와 알림 기준은 후속 작업이다.

## 관련 문서

- `docs/adr/0012-postgresql-lock-timeout-policy.md`
- `issue-drafts/0009-postgresql-lock-timeout-policy.md`
- `src/main/java/com/imwoo/airepo/wallet/api/WalletApiExceptionHandler.java`
