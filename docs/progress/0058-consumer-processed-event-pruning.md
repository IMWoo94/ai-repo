# 0058. Consumer Processed Event Pruning

## 스펙 목표

Consumer dedupe 저장소와 receipt 저장소가 무기한 증가하지 않도록 보존 기간 기반 pruning 정책과 운영 API를 추가한다.

## 완료 결과

- `OperationOutboxConsumerPruningPolicy`, `Service`, `Result`를 추가했다.
- `POST /api/v1/outbox-consumer/pruning-runs` API를 추가했다.
- consumer pruning API를 `ROLE_ADMIN` 변경성 운영 조치로 보호했다.
- 기본 비활성화 scheduler인 `OperationOutboxConsumerPruningScheduler`를 추가했다.
- processed-event 30일, receipt 30일 기본 보존 기간을 적용했다.

## 검증

- `./gradlew test --tests "*OperationOutboxConsumerPruningControllerTest" --tests "*OperationOutboxConsumerPruningServiceTest" --tests "*OperationOutboxConsumerPruningSchedulerTest" --tests "*JdbcWalletRepositoryTest"`

## 남은 일

- pruning 실행 이력 저장
- broker-specific replay window별 retention 권장값
- delivery metric bucket pruning

## 관련 문서

- `docs/adr/0048-consumer-processed-event-pruning.md`
- `docs/releases/unreleased.md`
- `wiki-drafts/Architecture-Decisions.md`
