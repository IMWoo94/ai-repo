# Scenario Test Strategy

## 목적

시나리오 테스트는 단위 기능의 정답보다 “대표 업무 흐름이 끝까지 연결되는가”를 검증한다.

일반 테스트가 도메인 객체, service, repository, API 단위를 촘촘히 확인한다면, 시나리오 테스트는 다음을 확인한다.

- 사용자 관점에서 요청 순서가 자연스럽게 이어지는가?
- 돈 이동 결과가 잔액, 거래내역, 원장, 감사, step log, outbox에 모두 남는가?
- 운영 조치가 manual review, requeue, audit trail로 이어지는가?
- 릴리스 전에 QA가 확인할 대표 흐름이 코드로 고정되어 있는가?

## 실행 명령

```bash
./gradlew scenarioTest
./gradlew postgresScenarioTest
```

일반 `test` task는 `scenario` tag를 제외한다. 시나리오 테스트는 `@Tag("scenario")`를 붙이고 `scenarioTest` task에서만 실행한다.
PostgreSQL profile과 Testcontainers가 필요한 시나리오는 `@Tag("postgres-scenario")`를 붙이고 `postgresScenarioTest` task에서만 실행한다.

백엔드/프론트 전체 테스트 실행 순서와 CI 대응 관계는 `local-test-guide.md`를 따른다.

## 시나리오 선정 기준

다음 중 하나 이상에 해당하면 시나리오 테스트 후보로 본다.

- 여러 API 또는 여러 application service가 이어진다.
- 잔액/원장/감사/outbox처럼 정합성 증거가 여러 저장소에 남는다.
- 운영자가 장애 조치 또는 재처리를 수행한다.
- 릴리스 노트에 포함될 대표 기능이다.
- QA가 수동으로 반복 검증할 가능성이 높다.

## 현재 시나리오

| 시나리오 | 파일 | 검증 |
| --- | --- | --- |
| 돈 이동 증거 흐름 | `WalletScenarioFlowTest` | 잔액 조회 → 충전 → 멱등 재시도 → 송금 → 잔액/원장/감사/step/outbox 조회 |
| Outbox 운영 흐름 | `WalletScenarioFlowTest` | outbox 실패 누적 → manual review 조회 → requeue → requeue audit 조회 |
| Outbox 발행 흐름 | `WalletScenarioFlowTest` | outbox event 생성 → fake publisher 발행 → `PUBLISHED` 상태 조회 |
| PostgreSQL profile 돈 이동 증거 흐름 | `PostgresWalletScenarioFlowTest` | Testcontainers PostgreSQL → Flyway → 충전/멱등 재시도/송금 → 원장/감사/step/outbox 발행 |

## 작성 규칙

- 테스트 클래스는 `src/test/java/.../scenario` 아래에 둔다.
- 클래스 또는 메서드에 `@Tag("scenario")`를 붙인다.
- 테스트 이름은 업무 흐름을 설명한다.
- 하나의 시나리오는 Given/When/Then보다 실제 흐름 순서가 드러나게 작성한다.
- 이미 계층 테스트가 검증하는 세부 예외를 반복하지 않는다.
- 외부 인프라가 필요한 시나리오는 기본 `scenario` tag에 포함하지 않고 별도 tag/task로 분리한다.
- PostgreSQL profile 시나리오는 `postgres-scenario` tag를 사용하고 Docker/Testcontainers 의존성을 문서화한다.

## 확장 후보

- HTTP broker adapter publish contract
- Kafka/RabbitMQ/SQS adapter publish scenario
- 관리자 인증/인가 후 manual review 권한 scenario
- release smoke scenario
