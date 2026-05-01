# [Quality] 검증 요약 기반 코드 하드닝

GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/41

## 배경

`_workspace/00_validation_summary.md` 검증 요약에서 v0.6.0 기준선의 잔여 결함과 probe 테스트가 정리되었다. 특히 원장 조회의 계정 상태 가드 누락, 실패 정책 회귀 보호 부재, 미존재 operation 조회 정책 빈틈은 코드와 테스트로 바로 보강할 수 있다.

## 목표

- 원장 조회에도 회원/지갑 queryable 가드를 동일하게 적용한다.
- 실패 요청이 step log/outbox를 남기지 않는 정책을 정식 테스트로 보호한다.
- 미존재 operation step/outbox 조회는 빈 결과가 아니라 명시적 Not Found로 처리한다.
- 검증 요약의 일부 probe를 정식 테스트로 이전한다.
- 문서에는 적용한 개선과 의도적으로 미루는 항목을 구분해 남긴다.

## 범위

- application 계층 조회 가드 정리
- operation not found 정책 추가
- application/infra 테스트 보강
- ADR/Wiki/Progress/Issue draft 문서 갱신

## 범위 제외

- GitHub Wiki 실제 동기화
- Testcontainers 강제 실행 정책 변경
- broker adapter 도입
- 인증/인가 도입

## 수용 기준

- [ ] SUSPENDED/CLOSED 지갑 또는 비활성 회원 소유 지갑의 원장 조회가 차단된다.
- [ ] 실패 요청이 step log/outbox를 남기지 않는 테스트가 정식 위치에 있다.
- [ ] 미존재 operation step/outbox 조회가 404 정책으로 보호된다.
- [ ] 트랜잭션 롤백 또는 fixtures 정합성 probe 중 실효성 높은 항목이 정식 테스트로 이전된다.
- [ ] `./gradlew test scenarioTest check`가 통과한다.
