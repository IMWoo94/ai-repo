# 2026-05-06 Full Review Improvement List

## 목적

현재 `v0.7.0` MVP 기준선 이후 프로젝트를 전면 검토하고, 다음 반복 작업 후보 10개를 추출한다.

## 검토 범위

- 백엔드 도메인, application, API, infra 코드
- React 사용자/운영자 화면
- 테스트 파이프라인과 smoke script
- 릴리스 노트, progress, Wiki draft, 기존 review 문서

## 현재 판단

`v0.7.0`은 1차 MVP 시연 기준선으로 충분하다. 잔액/거래내역, 충전/송금, 원장/감사, operation step log, outbox, manual review, PostgreSQL scenario CI, React 화면, GitHub Release, Wiki publish까지 연결되어 있다.

다만 다음 단계는 “더 많은 기능”보다 운영 하네스의 신뢰도를 올리는 방향이 우선이다. 특히 운영자 조치, broker 계약, consumer idempotency, 관측/알림, 프론트 구조 분리가 다음 품질 병목이다.

## 개선 후보 10개

| # | 우선순위 | 영역 | 개선 항목 | 근거 | 기대 효과 | 권장 검증 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | P0 | QA/E2E | manual review requeue 성공 E2E fixture 추가 | `v0.7.0` release note가 requeue 성공 E2E fixture 부재를 명시한다. 운영자 콘솔 E2E는 empty/error state까지만 검증한다. | 운영자 화면에서 조회 → 선택 → requeue → audit 확인까지 실제 브라우저 회귀 보호가 생긴다. | `npm --prefix frontend run e2e`, requeue fixture seed, backend scenario 연계 |
| 2 | P0 | 운영 UI | relay health/pruning 운영자 화면 추가 | 현재 미완료 목록에 relay health/pruning 화면이 남아 있고, backend API는 이미 존재한다. | 운영자가 scheduler 상태, relay run, pruning 결과를 화면에서 확인할 수 있다. | Vitest component test, Playwright operator console smoke |
| 3 | P0 | 보안/권한 | operator/admin token 분리와 role model 강화 | `AdminHeaderAuthenticationFilter`는 operator id가 있으면 OPERATOR와 ADMIN 권한을 모두 부여한다. | 조회 운영자와 관리자 조치 권한이 분리되어 pruning/requeue 같은 위험 조치가 더 명확히 보호된다. | Spring Security API test, 401/403 matrix test |
| 4 | P1 | 운영 절차 | requeue 승인 워크플로우 도입 | release note와 progress 미완료 목록에 운영자 승인 워크플로우가 남아 있다. 현재 requeue는 단일 operator action이다. | 금융권 운영 조치의 4-eyes 원칙을 학습 모델에 반영한다. | service test, audit trail test, operator UI approval E2E |
| 5 | P1 | 이벤트/정합성 | consumer idempotency 모델 추가 | outbox publish는 있지만 consumer idempotency가 후속 후보로 남아 있다. | MSA 전환 시 중복 delivery에도 외부 소비자가 동일 event를 한 번만 처리하는 기준을 세운다. | consumer ledger/audit idempotency contract test |
| 6 | P1 | Broker | Kafka/RabbitMQ/SQS 중 하나의 broker-specific adapter와 Testcontainers contract 추가 | 현재 HTTP adapter는 있지만 broker-specific adapter와 Testcontainers 정책이 미완료다. | 실제 메시징 시스템과 outbox envelope 계약을 검증할 수 있다. | broker Testcontainers contract, retry/failure scenario |
| 7 | P1 | 관측/알림 | external alert channel 추가 | relay health summary/alert 판정은 있으나 external alert channel이 후속 후보로 남아 있다. | 장애 징후를 API 조회가 아니라 Slack/Webhook 등 외부 알림으로 관측하는 운영 흐름을 만든다. | fake webhook contract test, failure path test |
| 8 | P2 | 운영 이력 | pruning 실행 이력 저장과 조회 API 추가 | pruning은 삭제 결과를 반환하지만 progress 기준 미완료에 pruning 실행 이력이 남아 있다. | 운영 로그 삭제 자체도 감사 가능한 이벤트가 된다. | repository/service/controller test, retention boundary test |
| 9 | P2 | 프론트 구조 | `frontend/src/App.tsx` 단일 컴포넌트 분리 | `App.tsx`가 580줄이고 API type, request client, wallet UI, operator UI가 한 파일에 있다. | 화면 변경 시 회귀 범위를 줄이고 테스트 단위를 작게 만든다. | `npm run test`, `npm run build`, component-level tests |
| 10 | P2 | 저장소 구조 | `JdbcWalletRepository` 책임 분리 | `JdbcWalletRepository`가 1206줄이고 wallet command/query, ledger, outbox, relay run, audit repository 역할을 모두 가진다. | DB 접근 책임을 bounded context별로 나눠 유지보수성과 리뷰 가능성을 높인다. | repository slice tests, `./gradlew test`, `postgresScenarioTest` |

## 상세 근거

### 1. manual review requeue 성공 E2E fixture

- `docs/releases/v0.7.0.md`는 운영자 requeue 성공 E2E fixture 부재를 알려진 제약으로 기록한다.
- `frontend/e2e/wallet-flow.spec.ts`는 운영자 콘솔에서 인증 오류와 empty state를 확인하지만, manual review fixture가 있는 성공 requeue 흐름은 없다.

권장 작업:

1. 테스트 전용 fixture 또는 API hook으로 `MANUAL_REVIEW` outbox event를 생성한다.
2. Playwright에서 manual review 조회, event 선택, requeue, audit 표시까지 검증한다.
3. backend scenario와 frontend E2E가 같은 용어를 사용하도록 QA 문서를 갱신한다.

### 2. relay health/pruning 운영자 화면

- `docs/progress/README.md`의 미완료 목록에 운영자 relay health/pruning 화면이 남아 있다.
- backend에는 relay run, health summary, operational pruning API가 있지만 React 화면에서는 manual review 중심으로만 노출된다.

권장 작업:

1. 운영자 콘솔에 relay health 카드, relay run table, pruning 실행 form을 추가한다.
2. 상태 badge는 기존 Apple 스타일 기준으로 정상/경고/위험을 구분한다.
3. API 실패, empty state, 권한 실패를 화면에서 분리한다.

### 3. operator/admin token 분리와 role model 강화

- `AdminHeaderAuthenticationFilter`는 operator id가 있으면 `ROLE_OPERATOR`, `ROLE_ADMIN`을 함께 부여한다.
- `SecurityConfig`는 pruning API만 `ADMIN`, 나머지는 `OPERATOR`로 구분하지만 현재 인증 소스가 같은 token/header라 실질 분리가 약하다.

권장 작업:

1. operator token과 admin token을 별도 property로 분리한다.
2. operator token은 조회/리뷰만, admin token은 pruning/승인/강제 requeue만 허용한다.
3. 401/403 test matrix를 명시적으로 추가한다.

### 4. requeue 승인 워크플로우

- 현재 requeue는 단일 `POST /api/v1/outbox-events/{id}/requeue` 호출로 끝난다.
- 금융권 운영 절차 관점에서는 요청자와 승인자를 분리하는 4-eyes model이 더 타당하다.

권장 작업:

1. requeue request 상태를 `REQUESTED`, `APPROVED`, `REJECTED`, `EXECUTED`로 분리한다.
2. 승인자는 요청자와 달라야 한다는 불변식을 둔다.
3. audit trail에 request/approval/execution을 모두 남긴다.

### 5. consumer idempotency 모델

- 현재 outbox는 publish 상태와 retry를 관리하지만, consumer 쪽 중복 처리 방지는 모델링되어 있지 않다.
- MSA 전환 시 logical transaction 정합성의 핵심은 producer outbox와 consumer idempotency의 쌍이다.

권장 작업:

1. `eventId` 또는 `idempotencyKey` 기반 consumed event registry를 설계한다.
2. consumer handler가 같은 event를 두 번 받아도 ledger/audit을 중복 생성하지 않게 한다.
3. contract test로 duplicate delivery를 고정한다.

### 6. broker-specific adapter와 Testcontainers contract

- HTTP adapter는 좋은 중간 단계지만 Kafka/RabbitMQ/SQS와 같은 실제 broker semantics를 검증하지는 않는다.
- broker별 ack, retry, ordering, partition/key 정책은 금융 이벤트 처리에서 중요한 tradeoff다.

권장 작업:

1. 우선 Kafka 또는 RabbitMQ 중 하나를 선택하는 ADR을 작성한다.
2. adapter module과 contract test를 추가한다.
3. Testcontainers 실행 비용과 CI 안정성을 release gate로 평가한다.

### 7. external alert channel

- relay health summary/alert 판정은 존재하지만 외부 알림 전송 경로는 없다.
- 운영자는 dashboard를 계속 보고 있지 않으므로 external alert channel이 필요하다.

권장 작업:

1. `AlertPublisher` port를 정의한다.
2. local fake/webhook adapter를 먼저 구현한다.
3. relay health degraded/manual review surge 조건에서 alert가 한 번만 발행되는지 검증한다.

### 8. pruning 실행 이력

- operational log pruning은 삭제 결과를 반환하지만 삭제 실행 자체를 장기 감사 로그로 남기는 구조는 약하다.
- 금융/운영 도메인에서는 “누가 언제 어떤 보존 정책으로 삭제했는가”가 중요하다.

권장 작업:

1. pruning run table을 추가한다.
2. 실행자, retention, cutoff, deleted counts, status를 저장한다.
3. 운영자 화면과 API에서 최근 pruning run을 조회한다.

### 9. React App 분리

- `frontend/src/App.tsx`는 580줄이며 domain type, API client, state orchestration, user UI, operator UI가 결합되어 있다.
- 지금은 MVP라 허용 가능하지만 다음 운영 화면 추가 시 변경 충돌과 테스트 비용이 커진다.

권장 작업:

1. `api/client.ts`, `types/wallet.ts`, `components/WalletWorkspace.tsx`, `components/OperatorConsole.tsx`로 분리한다.
2. UI state hook을 사용자 흐름과 운영자 흐름으로 나눈다.
3. 기존 테스트는 behavior 중심으로 유지한다.

### 10. JdbcWalletRepository 책임 분리

- `JdbcWalletRepository`는 1206줄이며 여러 repository interface를 한 클래스에서 구현한다.
- PostgreSQL 경로가 핵심 검증 축이 된 만큼, 작은 repository 단위가 코드 리뷰와 장애 분석에 유리하다.

권장 작업:

1. wallet command/query, ledger query, outbox relay, relay run, admin audit를 파일 단위로 분리한다.
2. 공통 row mapper와 timestamp 변환은 package-private helper로 둔다.
3. 기존 repository tests를 slice별로 재배치하되 scenario test는 그대로 유지한다.

## 우선 실행 순서 제안

1. `#1 manual review requeue 성공 E2E fixture`
2. `#2 relay health/pruning 운영자 화면`
3. `#3 operator/admin token 분리`
4. `#4 requeue 승인 워크플로우`
5. `#5 consumer idempotency 모델`

이 순서가 타당한 이유는 다음과 같다.

- 운영자 화면의 신뢰도와 권한 경계를 먼저 강화한다.
- 이후 MSA 전환을 대비해 producer/consumer 정합성 모델을 보강한다.
- 대규모 구조 분리인 프론트/Repository refactor는 기능 회귀 테스트가 더 안정된 뒤 진행한다.

## 실행 상태

- Top 3 항목은 `0046-top3-operational-hardening` 작업에서 구현 대상으로 선택했다.
- 남은 우선 후보는 requeue 승인 워크플로우, consumer idempotency, broker-specific adapter, external alert channel이다.

## 이번 검토에서 실행한 확인 명령

```bash
git status --short --branch
rg --files -g '!frontend/node_modules/**' -g '!frontend/dist/**'
wc -l frontend/src/App.tsx frontend/src/styles.css src/main/java/com/imwoo/airepo/wallet/infra/JdbcWalletRepository.java src/main/java/com/imwoo/airepo/wallet/infra/InMemoryWalletRepository.java
rg -n "TODO|FIXME|claimReady|requeueManual|deleteOutbox|FOR UPDATE|SKIP LOCKED|idempot|limit|MANUAL_REVIEW|lease" src/main/java src/test/java -g '*.java'
```

## 검토 한계

- 이번 작업은 개선 리스트업이 목적이므로 전체 테스트를 재실행하지 않았다.
- 실제 성능 병목, DB explain plan, 브라우저 접근성 점수는 별도 검증이 필요하다.
- 보안 평가는 코드 구조 관점의 후보 추출이며, 별도 threat modeling은 후속 작업으로 분리한다.
