# [Feature] Outbox Relay State and Retry Metadata

## 작업 요약

Transactional Outbox에 relay가 사용할 상태 전이 경계를 추가한다. `PENDING` outbox event를 조회하고, 발행 성공 시 `PUBLISHED`, 발행 실패 시 `FAILED`로 전이하며 attempt count, published time, last error를 기록한다.

## 배경과 문제

Issue #21에서 충전/송금 성공 결과를 `operation_outbox_events`에 `PENDING`으로 적재했다. 이로써 돈 이동 결과와 이벤트 적재는 같은 DB 트랜잭션 안에 묶였다.

하지만 현재 outbox event는 적재만 가능하고 relay/publisher가 사용할 상태 전이 모델이 없다. 추후 Kafka, RabbitMQ, SQS 같은 브로커를 붙이려면 다음 질문에 답해야 한다.

- 어떤 event가 아직 발행 대기 상태인가?
- 발행 성공 시 어떤 시간에 완료됐는가?
- 발행 실패 시 실패 횟수와 마지막 오류는 무엇인가?
- relay가 재시작돼도 같은 event를 추적할 수 있는가?

## 작업 유형

Outbox relay 기반 / 상태 전이 / 운영 관측

## 도메인 영역

충전, 송금, 이벤트 발행 경계, MSA 전환 준비

## 범위

### 하는 것

- `OperationOutboxEvent`에 `attemptCount`, `publishedAt`, `lastError` 메타데이터를 추가한다.
- pending outbox event 조회 경계를 추가한다.
- outbox event를 `PUBLISHED`로 전이하는 경계를 추가한다.
- outbox event를 `FAILED`로 전이하고 attempt count와 last error를 기록하는 경계를 추가한다.
- PostgreSQL에는 Flyway `V5` migration으로 outbox relay 메타데이터 컬럼을 추가한다.
- H2 빠른 저장소 테스트용 `schema.sql`도 같은 컬럼을 반영한다.
- ADR, Wiki, issue draft에 relay 상태 전이와 한계를 기록한다.

### 하지 않는 것

- 실제 메시지 브로커 발행은 구현하지 않는다.
- background scheduler를 구현하지 않는다.
- row claiming, `SKIP LOCKED`, multi-relay 병렬 처리는 구현하지 않는다.
- 자동 재시도 backoff 정책은 구현하지 않는다.

## 수용 기준

- [ ] 신규 outbox event는 `PENDING`, attempt count `0`, published time `null`, last error `null`로 저장된다.
- [ ] pending event를 제한 개수만큼 조회할 수 있다.
- [ ] 발행 성공 처리 시 event는 `PUBLISHED`가 되고 published time이 기록된다.
- [ ] 발행 실패 처리 시 event는 `FAILED`가 되고 attempt count가 증가하며 last error가 기록된다.
- [ ] PostgreSQL migration과 H2 schema가 모두 갱신된다.
- [ ] `./gradlew check`가 통과한다.
- [ ] ADR/Wiki에 relay 상태 전이와 후속 한계가 기록되어 있다.

## 도메인 규칙과 불변식

- outbox event는 생성 시 항상 `PENDING`이다.
- `PUBLISHED` event는 발행 완료 시간 `publishedAt`을 가져야 한다.
- `FAILED` event는 마지막 오류 `lastError`와 증가된 attempt count를 가져야 한다.
- relay 상태 전이는 돈 이동 결과를 변경하지 않는다.

## 하네스 역할 체크

- [x] 기획자 관점에서 outbox relay가 확인해야 할 운영 상태를 정의했다.
- [x] 도메인 전문가 관점에서 돈 이동 결과와 이벤트 발행 상태의 책임 분리를 검토했다.
- [x] 코드 개발자 A 관점에서 repository/service 경계를 추가하기로 했다.
- [x] 코드 개발자 B 관점에서 실제 브로커 발행과 `SKIP LOCKED` 병렬 처리는 범위 밖으로 제한했다.
- [x] QA 관점에서 pending 조회, published 전이, failed 전이 테스트 범위를 잡았다.
- [x] 릴리스 관리자 관점에서 Flyway migration 필요성을 확인했다.

## 예상 테스트 범위

- [x] 도메인 테스트가 필요하다.
- [x] Application service 테스트가 필요하다.
- [x] Repository 통합 테스트가 필요하다.
- [x] 회귀 테스트가 필요하다.
- [x] 릴리스 실행 검증이 필요하다.
- [ ] 코드 변경이 없는 문서 작업이다.

## 문서화 필요 여부

- [x] ADR이 필요하다.
- [x] Wiki 사고 과정 기록이 필요하다.
- [ ] Local Setup 갱신이 필요하다.
- [ ] PR 설명만으로 충분하다.

## 대안과 트레이드오프

### 대안 A: `PENDING` 적재만 유지

장점:

- 구현이 단순하다.
- 현재 이벤트 발행을 하지 않으므로 충분해 보일 수 있다.

단점:

- relay가 어떤 event를 처리했고 실패했는지 기록할 수 없다.
- 운영 관측과 장애 복구 기준이 부족하다.

### 대안 B: 상태 전이와 attempt 메타데이터 추가

장점:

- relay/publisher 구현 전에도 상태 모델을 검증할 수 있다.
- 성공/실패 처리를 DB 상태로 남겨 운영 분석이 가능하다.
- 추후 background relay를 붙일 때 경계가 명확하다.

단점:

- 아직 실제 발행과 재시도 backoff는 없다.
- 병렬 relay 안전성을 위한 claiming 정책은 후속으로 필요하다.

### 대안 C: relay와 `SKIP LOCKED` claiming까지 즉시 구현

장점:

- 실제 운영에 가까운 outbox relay 구조를 검증할 수 있다.
- 다중 relay 병렬 처리까지 고려할 수 있다.

단점:

- 현재 단계 대비 범위가 크다.
- scheduler, claiming timeout, duplicate publish 대응까지 함께 설계해야 한다.

### 현재 선호안

상태 전이와 attempt 메타데이터를 먼저 추가한다. 실제 relay는 후속 작업으로 남기되, relay가 사용할 저장소 경계를 코드와 테스트로 검증한다.

## 릴리스 고려사항

- 실행 검증: `./gradlew check`.
- PostgreSQL 실행 검증: Testcontainers 기반 저장소 테스트.
- DB 변경: Flyway `V5__add_outbox_relay_state.sql` migration 필요.

## DECIDE_LATER

- `SKIP LOCKED` 기반 claiming 정책.
- retry backoff와 재처리 스케줄.
- event payload schema versioning.
- 실제 broker adapter 구현.
