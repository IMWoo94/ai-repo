# ADR-0029: Outbox Relay Scheduler

## 상태

Accepted

## 맥락

Outbox publisher port를 통해 relay는 `publishReadyEvents(limit)`로 claim → publish → published/failed 흐름을 실행할 수 있다. 하지만 이 흐름은 수동 호출이나 테스트에서만 실행되며, 애플리케이션 안에서 주기적으로 발행 대기 event를 처리하는 worker 경계는 아직 없다.

금융/핀테크 서비스에서는 트랜잭션과 외부 이벤트 발행을 분리하더라도 발행 대기 event가 자동으로 처리되어야 한다. 동시에 로컬 학습/검증 환경에서는 자동 worker가 수동 테스트 결과를 바꾸지 않도록 명시적으로 켜고 끌 수 있어야 한다.

## 선택지

### 선택지 A: 수동 호출만 유지한다

장점:

- 테스트와 로컬 검증이 단순하다.
- background 실행에 따른 상태 변화가 없다.

단점:

- outbox event가 자동으로 발행되지 않는다.
- 운영 구조와 거리가 있다.
- relay 처리 주기와 batch size 정책을 검증할 수 없다.

### 선택지 B: 설정 기반 Spring scheduler를 추가한다

장점:

- 애플리케이션 안에서 주기적으로 outbox relay를 실행할 수 있다.
- `enabled=false` 기본값으로 로컬 수동 검증에 간섭하지 않는다.
- batch size와 실행 주기를 설정으로 분리할 수 있다.

단점:

- 단일 애플리케이션 인스턴스 기준 worker다.
- 다중 인스턴스 운영에서는 중복 scheduler가 실행될 수 있으므로 repository claim lock에 의존해야 한다.
- 실행 결과 모니터링과 알림은 별도 설계가 필요하다.

### 선택지 C: 별도 worker 프로세스로 분리한다

장점:

- API 서버와 worker lifecycle을 분리할 수 있다.
- 운영 환경에서 scale-out과 장애 격리를 설계하기 좋다.

단점:

- 현재 모놀리식 학습 단계에서는 배포/운영 복잡도가 커진다.
- 별도 process packaging, health check, release smoke 기준이 추가된다.

## 결정

설정 기반 Spring scheduler를 추가한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 실행 방식 | Spring `@Scheduled` |
| 기본값 | 비활성화 |
| 활성화 설정 | `AI_REPO_OUTBOX_RELAY_SCHEDULER_ENABLED=true` |
| batch size | `AI_REPO_OUTBOX_RELAY_BATCH_SIZE`, 기본 `10` |
| initial delay | `AI_REPO_OUTBOX_RELAY_INITIAL_DELAY_MS`, 기본 `5000` |
| fixed delay | `AI_REPO_OUTBOX_RELAY_FIXED_DELAY_MS`, 기본 `5000` |
| 실행 대상 | `OperationOutboxRelayService.publishReadyEvents(batchSize)` |

## 결과

장점:

- outbox relay가 수동 batch API에서 자동 실행 가능한 worker 경계로 확장된다.
- 로컬 검증에서는 기본 비활성화로 예측 가능한 상태를 유지한다.
- 실제 broker adapter가 붙기 전에도 worker lifecycle과 batch 정책을 테스트할 수 있다.

비용:

- scheduler 실행 결과를 저장하거나 외부로 노출하지 않는다.
- 운영 모니터링, 알림, distributed singleton worker 보장은 아직 없다.

후속 작업:

- 실제 broker adapter 도입 후 scheduler 활성화 smoke test를 추가한다.
- scheduler 실행 결과 metric과 failure alert 기준을 만든다.
- 다중 인스턴스 운영 시 distributed lock 또는 worker 분리 여부를 재검토한다.
