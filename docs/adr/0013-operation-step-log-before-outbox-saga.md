# ADR-0013: Operation Step Log Before Outbox and Saga

## 상태

Accepted

## 맥락

현재 프로젝트는 모놀리식 Spring Boot 애플리케이션이다. 하지만 충전/송금은 추후 MSA로 분리될 가능성이 높은 도메인 흐름이며, 돈 이동 과정에서는 논리적 트랜잭션과 단계별 정합성 증거가 중요하다.

기존 기록은 다음 책임을 가진다.

- 거래내역: 사용자에게 보여줄 금융 활동 기록
- 원장: 잔액 정합성 검증 기록
- 감사 이벤트: 명령 완료에 대한 운영 기록

이 기록만으로는 하나의 명령이 내부에서 어떤 처리 단계를 거쳐 완료됐는지 설명하기 어렵다. MSA/Saga/Outbox로 전환하기 전에도 모놀리스 내부에서 단계 경계를 명시해야 이후 분리 비용이 낮아진다.

## 선택지

### 선택지 A: 감사 이벤트만 유지

장점:

- 구현이 단순하다.
- 저장소와 API 변경이 없다.

단점:

- 명령 처리 중간 단계의 관측 가능성이 부족하다.
- 장애 분석 시 어느 단계까지 완료됐는지 설명하기 어렵다.
- 추후 Saga/Outbox 경계를 별도로 다시 찾아야 한다.

### 선택지 B: Operation Step Log 추가

장점:

- 모놀리스 안에서 논리적 트랜잭션의 단계별 완료 증거를 남긴다.
- 원장, 감사 이벤트와 별도로 처리 과정 관측 책임을 분리한다.
- 추후 MSA 전환 시 Saga step 또는 Outbox event 후보를 식별하기 쉽다.

단점:

- 기록 테이블과 조회 API가 추가된다.
- 현재는 성공 단계 중심이며 실패/보상 단계는 후속 설계가 필요하다.

### 선택지 C: Outbox/Saga 즉시 도입

장점:

- MSA 전환에 가까운 구조를 바로 실험할 수 있다.
- 메시지 발행, 재시도, 보상 처리까지 함께 설계할 수 있다.

단점:

- 현재 모놀리스 학습 단계에 비해 범위가 크다.
- 메시지 브로커, outbox relay, idempotent consumer까지 설계해야 한다.
- 핵심 도메인 학습보다 인프라 복잡도가 먼저 커진다.

## 결정

`Operation Step Log 추가`를 선택한다.

초기 구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 기록 대상 | 충전/송금 성공 처리 단계 |
| 단계 | `BALANCE_LOCKED`, `BALANCE_UPDATED`, `TRANSACTION_RECORDED`, `LEDGER_RECORDED`, `AUDIT_RECORDED`, `IDEMPOTENCY_RECORDED` |
| 상태 | 1차 범위는 `COMPLETED` |
| 저장소 | 인메모리와 PostgreSQL 모두 지원 |
| DB 변경 | Flyway `V3__create_operation_step_logs.sql` |
| 조회 API | `GET /api/v1/operations/{operationId}/step-logs` |
| 실패 정책 | 실패한 요청과 lock timeout 요청은 step log를 남기지 않음 |

## 결과

장점:

- 돈 이동 명령의 내부 처리 순서를 API와 DB로 확인할 수 있다.
- 감사 이벤트와 원장의 책임을 오염시키지 않고 관측 기록을 추가한다.
- 추후 Outbox/Saga 전환 전 단계 경계가 코드에 남는다.

비용:

- 기록량이 증가한다.
- 단계 정의가 바뀌면 문서와 테스트도 함께 갱신해야 한다.
- 실패/보상 단계는 아직 표현하지 않는다.

후속 작업:

- 실패 단계와 보상 단계 상태 모델을 추가할지 결정한다.
- Outbox event와 step log의 관계를 정의한다.
- operation id를 서비스 간 correlation id로 승격할지 검토한다.
