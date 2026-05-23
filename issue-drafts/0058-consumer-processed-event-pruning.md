# Consumer Processed Event Pruning

## 배경

Consumer processed-event 저장소는 duplicate side effect 방지에 필요하지만, 보존 기간이 없으면 시간이 지날수록 계속 증가한다.

## 목표

- processed-event와 receipt를 보존 기간 기준으로 삭제한다.
- 수동 pruning API는 admin 권한만 허용한다.
- scheduler는 기본 비활성화로 제공한다.
- 삭제 cutoff와 삭제 건수를 응답으로 확인한다.

## 완료 조건

- `POST /api/v1/outbox-consumer/pruning-runs`가 삭제 결과를 반환한다.
- operator token만으로는 403을 반환한다.
- cutoff보다 오래된 row만 삭제된다.
- service, scheduler, controller, repository test가 통과한다.

## 검증 명령

```bash
./gradlew test --tests "*OperationOutboxConsumerPruningControllerTest" --tests "*OperationOutboxConsumerPruningServiceTest" --tests "*OperationOutboxConsumerPruningSchedulerTest" --tests "*JdbcWalletRepositoryTest"
```
