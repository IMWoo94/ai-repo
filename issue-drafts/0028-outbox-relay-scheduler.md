# Outbox Relay Scheduler

## 배경

Outbox relay는 `publishReadyEvents(limit)` batch API를 통해 claim → publish → published/failed 흐름을 실행할 수 있다. 하지만 현재는 테스트나 수동 호출에서만 실행되며, 애플리케이션 안에서 주기적으로 실행되는 worker 경계가 없다.

금융/핀테크 서비스에서는 돈 이동 트랜잭션과 외부 이벤트 발행을 분리하더라도, 발행 대기 event가 자동으로 처리되는지와 자동 실행을 언제 켜고 끌 수 있는지가 중요하다.

## 목표

- Outbox relay scheduler를 추가한다.
- scheduler는 설정으로 명시적으로 켤 때만 동작한다.
- scheduler 실행마다 `OperationOutboxRelayService.publishReadyEvents(limit)`를 호출한다.
- batch size, initial delay, fixed delay를 설정으로 분리한다.
- 테스트와 문서로 자동 실행 경계를 검증한다.

## 범위

- Spring scheduling 활성화
- conditional scheduler bean 추가
- scheduler 설정값 추가
- scheduler 단위 테스트 추가
- ADR, progress report, local guide 갱신

## 제외 범위

- 실제 broker adapter
- 분산 lock 기반 singleton worker 보장
- 운영 모니터링/알림
- scheduler 실행 결과 API

## 완료 조건

- [ ] scheduler가 설정으로 켜고 꺼질 수 있다.
- [ ] scheduler가 설정된 batch size로 relay publish batch를 실행한다.
- [ ] 기본 로컬 실행에서는 scheduler가 비활성화되어 수동 검증에 간섭하지 않는다.
- [ ] scheduler 단위 테스트가 있다.
- [ ] ADR과 progress report가 추가된다.
- [ ] `./gradlew test scenarioTest check`가 통과한다.

## 검증

```bash
./gradlew test scenarioTest check
```
