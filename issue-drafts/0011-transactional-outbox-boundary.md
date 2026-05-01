# [Feature] Transactional Outbox Boundary for Wallet Operations

## 작업 요약

충전/송금 성공 결과를 같은 DB 트랜잭션 안에서 `operation_outbox_events`에 적재한다. 현재는 이벤트를 발행하지 않고 `PENDING` 상태로 저장만 하며, 추후 MSA 전환 시 Outbox relay 또는 메시지 브로커로 확장할 경계를 만든다.

## 배경과 문제

Issue #19에서 operation step log를 추가해 모놀리스 내부 처리 단계를 관측할 수 있게 했다. 하지만 step log는 처리 과정 관측 기록이지 외부 서비스로 전달할 integration event가 아니다.

추후 MSA로 전환하면 송금 성공 후 다음과 같은 외부 반응이 필요할 수 있다.

- 알림 서비스에 송금 완료 이벤트 전달
- 정산/리포팅 서비스에 돈 이동 결과 전달
- 이상거래 탐지 서비스에 거래 이벤트 전달
- 장애 후 outbox relay가 미발행 이벤트를 재처리

현재 단계에서 Kafka나 relay를 도입하면 범위가 커지므로, 우선 성공한 돈 이동 결과를 같은 트랜잭션 안에 outbox event로 저장하는 경계를 만든다.

## 작업 유형

정합성 / MSA 전환 기반 / Outbox

## 도메인 영역

충전, 송금, 논리적 트랜잭션, 이벤트 발행 경계

## 범위

### 하는 것

- `OperationOutboxEvent` 도메인 모델을 추가한다.
- 충전/송금 성공 시 같은 저장소 트랜잭션 안에서 outbox event를 `PENDING`으로 저장한다.
- PostgreSQL에는 Flyway `V4` migration으로 `operation_outbox_events` 테이블을 추가한다.
- H2 빠른 저장소 테스트용 `schema.sql`도 같은 테이블을 반영한다.
- API에서 operation id 기준 outbox event를 조회한다.
- ADR, Wiki, issue draft에 step log와 outbox의 책임 경계를 기록한다.

### 하지 않는 것

- Kafka, RabbitMQ, SQS 같은 메시지 브로커를 도입하지 않는다.
- Outbox relay/publisher를 구현하지 않는다.
- 이벤트 전송 성공/실패 상태 전이를 구현하지 않는다.
- consumer idempotency를 구현하지 않는다.

## 수용 기준

- [ ] 충전 성공 시 outbox event가 1건 생성된다.
- [ ] 송금 성공 시 outbox event가 1건 생성된다.
- [ ] 멱등 재시도는 outbox event를 중복 생성하지 않는다.
- [ ] lock timeout 또는 실패한 요청은 outbox event를 생성하지 않는다.
- [ ] `GET /api/v1/operations/{operationId}/outbox-events`로 outbox event를 조회할 수 있다.
- [ ] PostgreSQL migration과 H2 schema가 모두 갱신된다.
- [ ] `./gradlew check`가 통과한다.
- [ ] ADR/Wiki에 step log와 outbox event의 책임 경계가 기록되어 있다.

## 도메인 규칙과 불변식

- outbox event는 외부 시스템으로 전달할 integration event 후보이다.
- outbox event는 돈 이동 결과와 같은 DB 트랜잭션 안에 저장되어야 한다.
- step log는 처리 과정 관측 기록이고, outbox event는 외부 반응을 위한 전달 후보이다.
- 멱등 재시도는 기존 operation 결과를 반환하며 outbox event를 중복 생성하지 않는다.

## 하네스 역할 체크

- [x] 기획자 관점에서 MSA 전환 시 외부 서비스 반응 경계를 정의했다.
- [x] 도메인 전문가 관점에서 step log, audit, outbox의 책임 분리를 검토했다.
- [x] 코드 개발자 A 관점에서 현재 저장소 트랜잭션 안에서 outbox 적재를 구현하기로 했다.
- [x] 코드 개발자 B 관점에서 relay와 브로커 도입은 범위 밖으로 제한했다.
- [x] QA 관점에서 성공, 멱등 재시도, 실패 요청의 outbox 생성 여부를 검증 범위로 잡았다.
- [x] 릴리스 관리자 관점에서 Flyway migration 필요성을 확인했다.

## 예상 테스트 범위

- [x] 도메인 테스트가 필요하다.
- [x] API 테스트가 필요하다.
- [x] Repository 통합 테스트가 필요하다.
- [x] 멱등성 테스트가 필요하다.
- [x] 회귀 테스트가 필요하다.
- [x] 릴리스 실행 검증이 필요하다.
- [ ] 코드 변경이 없는 문서 작업이다.

## 문서화 필요 여부

- [x] ADR이 필요하다.
- [x] Wiki 사고 과정 기록이 필요하다.
- [ ] Local Setup 갱신이 필요하다.
- [ ] PR 설명만으로 충분하다.

## 대안과 트레이드오프

### 대안 A: Step Log만 유지

장점:

- 구현이 단순하다.
- 기록 종류가 늘어나지 않는다.

단점:

- 외부 서비스로 전달할 이벤트 경계가 없다.
- step log를 integration event로 오용할 위험이 있다.

### 대안 B: Transactional Outbox 저장만 추가

장점:

- 돈 이동 결과와 이벤트 적재를 같은 DB 트랜잭션으로 묶는다.
- 메시지 브로커 없이도 MSA 전환 경계를 코드로 남긴다.
- relay/publisher를 후속 작업으로 분리할 수 있다.

단점:

- 아직 실제 발행은 하지 않는다.
- event schema versioning과 retry 정책이 후속으로 필요하다.

### 대안 C: Kafka/Saga/Relay 즉시 도입

장점:

- 분산 시스템 전환을 더 실제적으로 검증할 수 있다.
- 발행, 재시도, consumer 처리까지 end-to-end로 볼 수 있다.

단점:

- 현재 모놀리스 학습 단계에 비해 인프라 범위가 크다.
- 브로커 운영, relay, consumer idempotency까지 설계해야 한다.

### 현재 선호안

Transactional Outbox 저장만 먼저 추가한다. 현재 목표는 메시지 인프라 도입이 아니라, 돈 이동 결과와 외부 반응 후보를 같은 트랜잭션 안에서 정합성 있게 남기는 것이다.

## 릴리스 고려사항

- 실행 검증: `./gradlew check`.
- PostgreSQL 실행 검증: Testcontainers 기반 저장소 테스트.
- DB 변경: Flyway `V4__create_operation_outbox_events.sql` migration 필요.

## DECIDE_LATER

- Outbox relay/publisher 구현 방식.
- event payload schema와 versioning 정책.
- consumer idempotency key 정책.
- outbox event 재시도/폐기 상태 전이.
