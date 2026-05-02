# 0028. Outbox Relay Scheduler

## 스펙 목표

- Outbox relay를 애플리케이션 안에서 주기적으로 실행할 수 있는 scheduler 경계를 추가한다.
- 로컬 수동 검증에 간섭하지 않도록 기본값은 비활성화한다.
- batch size와 실행 주기를 설정으로 분리한다.

## 완료 결과

- `@EnableScheduling`을 애플리케이션에 추가했다.
- `OperationOutboxRelayScheduler`를 추가했다.
- scheduler는 `ai-repo.outbox-relay.scheduler.enabled=true`일 때만 bean으로 등록된다.
- scheduler는 `OperationOutboxRelayService.publishReadyEvents(batchSize)`를 호출한다.
- batch size가 0 이하이면 애플리케이션 설정 오류로 실패한다.
- scheduler 단위 테스트로 batch size 전달과 잘못된 batch size를 검증했다.
- README, ADR, progress report, local setup 문서를 갱신했다.

## 검증

- `./gradlew test --tests '*OperationOutboxRelaySchedulerTest' --tests '*WalletScenarioFlowTest'`

## 남은 일

- 실제 broker adapter 도입 후 scheduler 활성화 smoke test를 추가한다.
- scheduler 실행 metric과 alert 기준을 만든다.
- 다중 인스턴스 운영 시 worker 분리 또는 distributed lock 정책을 검토한다.

## 관련 문서

- `docs/adr/0029-outbox-relay-scheduler.md`
- `docs/development/local-setup.md`
- `issue-drafts/0028-outbox-relay-scheduler.md`
