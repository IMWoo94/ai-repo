# ADR-0048: Consumer Processed Event Pruning

## 상태

Accepted

## 배경

Consumer processed-event 저장소는 `idempotencyKey` unique 제약으로 duplicate side effect를 막는다. 하지만 이 row를 영구 보관하면 broker replay가 많거나 운영 기간이 길어질수록 dedupe 저장소와 receipt 저장소가 계속 증가한다.

금융/핀테크 관점에서는 dedupe key 보존 기간이 broker replay 가능 기간보다 충분히 길어야 한다. 동시에 운영자는 오래된 consumer 관측 데이터를 수동 또는 scheduler로 정리할 수 있어야 한다.

## 결정

Consumer processed-event와 consumer receipt를 별도 consumer pruning 정책으로 정리한다.

| 항목 | 결정 |
| --- | --- |
| 수동 API | `POST /api/v1/outbox-consumer/pruning-runs` |
| 권한 | 변경성 운영 조치이므로 `ROLE_ADMIN` |
| 기본 보존 기간 | processed-event 30일, receipt 30일 |
| 삭제 기준 | `processed_at < cutoff`, `received_at < cutoff` |
| scheduler | 기본 비활성화, 명시적으로 켠 경우에만 실행 |

## 트레이드오프

### 장점

- dedupe/receipt 테이블의 무한 증가를 막는다.
- 운영자가 삭제 기준과 삭제 건수를 API 응답으로 확인할 수 있다.
- 기존 operational log pruning과 같은 운영 패턴을 유지한다.

### 비용

- 보존 기간보다 오래된 broker replay는 다시 side effect 대상이 될 수 있다.
- 따라서 실제 broker 도입 시 retention은 broker 재전송/재처리 가능 기간보다 길게 설정해야 한다.
- pruning 실행 이력의 장기 보존은 아직 별도 과제다.

## 검증 기준

- cutoff보다 오래된 processed-event와 receipt만 삭제된다.
- cutoff와 같은 시각의 row는 삭제되지 않는다.
- operator token만으로는 pruning API를 실행할 수 없다.
- service, scheduler, controller, JDBC repository 테스트가 통과한다.

## 후속 작업

- broker-specific replay window에 맞춘 retention 권장값을 문서화한다.
- pruning 실행 이력을 별도 운영 이력으로 저장한다.
- duplicate attempt history 또는 time bucket metric을 추가한다.
