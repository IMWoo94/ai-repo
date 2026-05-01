# [Feature] PostgreSQL Lock Timeout and Busy Wallet Policy

## 작업 요약

PostgreSQL 잔액 row lock 대기 시간이 길어질 때 무한 대기하지 않도록 저장소 트랜잭션에 lock timeout을 적용하고, API에는 재시도 가능한 지갑 경합 오류(`WALLET_BALANCE_BUSY`)로 반환하는 정책을 추가한다.

## 배경과 문제

Issue #15에서 PostgreSQL `SELECT ... FOR UPDATE` 기반 잔액 row lock을 도입했다. 이로써 동시 송금 시 잔액 음수 방지 불변식은 DB 트랜잭션 안에서 보호된다.

남은 문제는 lock 대기 정책이다. 같은 지갑에 요청이 몰리거나 긴 트랜잭션이 row lock을 오래 잡으면 후속 요청이 무기한에 가깝게 대기할 수 있다. 금융 서비스에서는 사용자가 응답 없이 오래 기다리는 것보다, 짧은 시간 안에 재시도 가능한 경합 오류를 받는 편이 운영과 UX 모두에서 예측 가능하다.

## 작업 유형

저장소 안정성 / API 오류 정책

## 도메인 영역

송금, 충전, 잔액, 동시성

## 범위

### 하는 것

- PostgreSQL JDBC 저장소 트랜잭션에 lock timeout을 설정한다.
- lock timeout 또는 lock 획득 실패를 도메인 예외로 변환한다.
- API 오류 코드 `WALLET_BALANCE_BUSY`를 추가한다.
- Testcontainers PostgreSQL 테스트로 lock timeout 변환을 검증한다.
- ADR, Wiki, issue draft에 timeout 값과 대안을 기록한다.

### 하지 않는 것

- 분산락을 도입하지 않는다.
- 사용자 재시도 스케줄링을 구현하지 않는다.
- 낙관적 락 version column을 추가하지 않는다.
- HTTP `Retry-After` 헤더 정책은 이번 범위에서 제외한다.

## 수용 기준

- [ ] PostgreSQL 저장소는 잔액 row lock 전에 lock timeout을 설정한다.
- [ ] lock timeout은 `WalletConcurrencyException`으로 변환된다.
- [ ] API는 `WalletConcurrencyException`을 `409 Conflict`와 `WALLET_BALANCE_BUSY`로 반환한다.
- [ ] Testcontainers PostgreSQL 테스트가 row lock 보유 상황에서 timeout 변환을 검증한다.
- [ ] `./gradlew check`가 통과한다.
- [ ] ADR/Wiki에 timeout 정책과 트레이드오프가 기록되어 있다.

## 도메인 규칙과 불변식

- lock timeout은 잔액 실패가 아니라 재시도 가능한 경합 실패다.
- lock timeout이 발생한 요청은 거래내역, 원장, 감사 로그, 멱등 기록을 남기지 않는다.
- 잔액 음수 방지 불변식은 계속 DB row lock과 트랜잭션 내부 재검증으로 지킨다.

## 하네스 역할 체크

- [x] 기획자 관점에서 사용자 대기 시간과 재시도 가능한 실패 응답을 분리했다.
- [x] 도메인 전문가 관점에서 잔액 부족과 lock 경합을 다른 실패로 정의했다.
- [x] 코드 개발자 A 관점에서 PostgreSQL lock timeout 설정 위치를 검토했다.
- [x] 코드 개발자 B 관점에서 H2 빠른 테스트와 PostgreSQL 전용 정책의 경계를 검토했다.
- [x] QA 관점에서 실제 row lock 보유 상황의 Testcontainers 검증 필요성을 확인했다.
- [x] 릴리스 관리자 관점에서 스키마 변경 없는 운영 정책 변경임을 확인했다.

## 예상 테스트 범위

- [ ] 단위 테스트가 필요하다.
- [x] API 오류 매핑 테스트가 필요하다.
- [x] Repository 통합 테스트가 필요하다.
- [x] 동시성 테스트가 필요하다.
- [x] 회귀 테스트가 필요하다.
- [x] 릴리스 실행 검증이 필요하다.
- [ ] 코드 변경이 없는 문서 작업이다.

## 문서화 필요 여부

- [x] ADR이 필요하다.
- [x] Wiki 사고 과정 기록이 필요하다.
- [ ] Local Setup 갱신이 필요하다.
- [ ] PR 설명만으로 충분하다.

## 대안과 트레이드오프

### 대안 A: lock timeout을 두지 않음

장점:

- 구현이 단순하다.
- 요청이 lock을 얻을 때까지 기다리므로 일시 경합에서 성공 가능성이 높다.

단점:

- 사용자 응답 시간이 예측 불가능하다.
- 장애 또는 긴 트랜잭션 상황에서 대기가 길어진다.
- 운영 관측과 알림 기준을 세우기 어렵다.

### 대안 B: 짧은 PostgreSQL lock timeout

장점:

- 경합 상황을 빠르게 실패로 전환할 수 있다.
- API가 재시도 가능한 busy 상태를 명확히 표현한다.
- DB row lock 정책과 자연스럽게 이어진다.

단점:

- 일시 경합에서도 실패 응답이 늘 수 있다.
- 적절한 timeout 값은 트래픽과 SLA에 맞춰 조정이 필요하다.

### 대안 C: 애플리케이션 레벨 retry

장점:

- 사용자가 보는 실패를 줄일 수 있다.
- 짧은 경합은 서버 안에서 흡수할 수 있다.

단점:

- 중복 요청, 멱등키, 트랜잭션 재시도 정책이 더 복잡해진다.
- 현재 학습 단계에서는 장애 원인 관측보다 숨김 효과가 크다.

### 현재 선호안

짧은 PostgreSQL lock timeout을 선택한다. 1차 기준은 1초로 두고, timeout이 발생하면 `WALLET_BALANCE_BUSY`로 반환한다.

## 릴리스 고려사항

- 실행 검증: `./gradlew check`.
- PostgreSQL 실행 검증: Testcontainers 기반 lock timeout 테스트.
- 알려진 리스크: 로컬 Docker가 꺼진 환경에서는 Testcontainers 테스트가 스킵될 수 있으므로 CI 결과를 확인한다.

## DECIDE_LATER

- timeout 값을 환경 변수로 조정할지 여부.
- `Retry-After` 헤더를 추가할지 여부.
- 서버 내부 자동 재시도 정책을 둘지 여부.
