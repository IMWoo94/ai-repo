# MVP Public Readiness Criteria

이 문서는 ai-repo의 현재 상태를 기준으로 MVP 선정 기준과 개선 기능 공개 기준을 재정의한다. 여기서 공개는 실제 금융 운영 배포가 아니라 외부 시연, 포트폴리오 공개, 로컬 또는 k8s 데모 기준의 공개 가능 상태를 뜻한다.

## 프로젝트 한줄 정의

ai-repo는 실제 금융 거래 시스템이 아니라, 지갑/원장/아웃박스/운영 관측 흐름을 작은 범위로 구현하고 문서, 테스트, 리뷰, 릴리스 기준까지 함께 검증하는 핀테크 엔지니어링 랩이다.

## MVP 목적

MVP는 "돈이 이동하는 핵심 흐름을 안전하게 설명하고, 실패/재처리/운영 확인까지 시연할 수 있는 최소 기준선"이다. 따라서 MVP의 가치는 기능 개수가 아니라 다음 세 가지를 증명하는 데 있다.

- 사용자 관점: 로그인 후 잔액 조회, 충전, 송금, 거래내역 확인이 화면과 API에서 재현된다.
- 정합성 관점: 돈 이동 결과가 잔액, 거래, 원장, 감사 로그, operation step log, outbox event로 추적된다.
- 운영 관점: relay/manual review/requeue/health/audit/pruning 흐름을 보호된 운영 API 또는 운영자 화면에서 확인할 수 있다.

## MVP 선정 기준

### Must Have

다음 항목 중 하나라도 빠지면 MVP가 아니라 기능 데모다.

- 핵심 지갑 흐름: 잔액 조회, 거래내역 조회, 충전, 송금이 API와 React 화면에서 모두 동작한다.
- 인증/소유권: 엔드유저 JWT 로그인과 wallet ownership 검사가 적용되어 비소유 지갑 접근이 차단된다.
- 돈 이동 정합성: 성공한 충전/송금은 잔액, 거래내역, 원장, 감사 로그, operation step log, outbox event로 추적된다.
- 멱등성과 동시성: Idempotency-Key 중복 요청, fingerprint conflict, PostgreSQL row lock, lock timeout, 2-wallet lock 순서가 테스트로 검증된다.
- PostgreSQL 운영 유사성: `postgres` profile이 Flyway migration과 Testcontainers scenario로 검증된다.
- 아웃박스 운영 흐름: outbox relay claim/retry/lease/max-attempt/manual-review 상태가 설명 가능하고 테스트된다.
- manual review 재처리: requeue 요청, 승인, 실행, 반려와 audit trail이 보호된 운영 경로로 동작한다.
- 직접 requeue 차단: workflow 우회를 막는 직접 requeue API는 `410 Gone`과 `DIRECT_REQUEUE_API_DEPRECATED`로 실패한다.
- 운영 API 보호: 조회성 운영 API는 operator/admin token, 변경성 운영 API는 admin token과 operator id로 보호된다.
- broker consumer 보호: `/internal/broker/outbox-events`는 `X-Broker-Token` shared secret을 요구하고, publisher도 같은 secret을 부착한다.
- 운영 관측: relay run, relay health, consumer metrics/window metrics/health, operational alerts, admin access audit, pruning 결과를 확인할 수 있다.
- 공개 검증 게이트: 백엔드, PostgreSQL scenario, 프론트 unit/build/E2E, local smoke, compose config, whitespace check가 통과해야 한다.
- 문서 정합성: README, architecture, ADR, progress, release note, local test guide가 현재 구현과 모순되지 않는다.

### Should Have

MVP 공개를 더 설득력 있게 만들지만, 누락 시 알려진 제약으로 명시하고 공개할 수 있다.

- 로컬 k8s 스택: 앱, PostgreSQL, Prometheus, Grafana, Loki가 `scripts/k8s-local-up.sh`로 기동되고 smoke로 확인된다.
- GitOps 데모: GitHub Actions, GHCR, ArgoCD 흐름이 문서와 manifest로 설명 가능하다.
- Slack webhook alert: warning/critical operational alert를 Slack Incoming Webhook 호환 endpoint로 push할 수 있다.
- operational alert suppression/pruning: 같은 alert 중복 저장 억제와 보존 기간 삭제가 테스트된다.
- 운영자 화면 확장: manual review뿐 아니라 relay health, pruning, operational alert까지 화면에서 확인할 수 있다.
- Wiki 공개 상태: `wiki-drafts`가 GitHub Wiki에 반영되고 Home, Release-Notes, QA-Scenarios, Architecture-Decisions가 열린다.
- opt-in ELK 학습 스택: PLG 기본 로그 스택과 별개로 Filebeat, Logstash, Elasticsearch, Kibana를 수동 기동해 로그 파싱을 시연할 수 있다.

### Defer

다음 항목은 가치가 있지만, MVP 핵심 학습 목표를 증명하는 데 필수는 아니다. 개선 release 후보로 둔다.

- Kafka, RabbitMQ, SQS 같은 broker-specific adapter와 Testcontainers contract.
- broker replay window별 retention 권장값 자동화.
- pruning 실행 이력 저장과 조회 API.
- Slack 발행 실패 record와 재시도 정책.
- 실제 identity provider 또는 운영자 principal/role scope 연동.
- JWT 만료 후 refresh token 또는 silent refresh 정책.
- 운영 alert 화면 연결.
- broker-specific 장애 주입과 복구 시나리오.
- `App.tsx` 모놀리식 구조 분해.
- JDBC adapter 추가 분해나 성능 최적화처럼 공개 데모 가치를 직접 바꾸지 않는 구조 개선.

### Explicitly Out of Scope

다음은 이 프로젝트의 공개 범위에 넣지 않는다.

- 실제 금융 거래 처리.
- 실명 인증, 고객 개인정보 처리, KYC/AML.
- 외부 결제망, 은행망, 카드망 연동.
- 실고객 대상 운영, SLA, 장애 대응 조직, 규제 보고.
- 완전한 복식부기/house account/정산 계정 모델.
- 이상거래 탐지 운영 정책의 실전 수준 구현.
- 운영자 헤더 토큰을 실제 사내 IAM으로 대체하는 운영 인증 체계.

## MVP 탈락 기준

아래 조건 중 하나라도 해당하면 MVP 공개를 보류한다.

- 핵심 사용자 흐름 중 잔액 조회, 충전, 송금, 거래내역 확인 중 하나가 API 또는 화면에서 실패한다.
- 돈 이동 결과가 원장, 감사 로그, step log, outbox event 중 하나 이상으로 추적되지 않는다.
- PostgreSQL profile 또는 Flyway migration이 깨져 `postgresScenarioTest`가 통과하지 않는다.
- 멱등 재시도, fingerprint conflict, 잔액 부족, lock timeout 같은 금융 정합성 경계가 테스트되지 않는다.
- 운영 API가 인증 없이 열리거나, 변경성 운영 API가 admin 권한 없이 실행된다.
- `/internal/broker/outbox-events`에 미인증 event 주입이 가능하다.
- direct requeue가 workflow를 우회해 성공한다.
- release gate 명령 중 실패가 있는데 known issue로 격리하지 않고 공개한다.
- README, release note, architecture 문서가 실제 구현과 다른 보안/운영 수준을 암시한다.
- 실제 금융 서비스처럼 오해될 수 있는 문구가 release note나 README에 남아 있다.

## 개선 기능 실오픈 기준

개선 기능은 "구현 완료"가 아니라 아래 기준을 통과해야 공개 후보가 된다.

### 사용자 가치

- 어떤 사용자 또는 운영자 문제가 줄어드는지 한 문장으로 설명된다.
- 공개 시연에서 확인할 수 있는 화면, API, 로그, 지표 중 하나 이상의 증거가 있다.
- 기존 MVP 흐름을 더 복잡하게 만들면 그 복잡도를 상쇄하는 운영/학습 가치가 명시된다.
- 단순 학습 호기심이면 release blocker가 아니라 P2 후보로 둔다.

### 도메인 정합성

- 돈 이동, 원장, 감사, outbox, idempotency 중 영향을 받는 경계가 명시된다.
- 성공/실패/중복/동시성/재시도/롤백 중 해당되는 케이스가 테스트된다.
- PostgreSQL profile에서 필요한 transaction boundary와 lock 정책이 검증된다.
- 기존 단순화, 예를 들어 충전 단측 원장이나 실패 감사 미기록 정책을 바꾸면 ADR 또는 release note에 근거가 남는다.

### 보안/권한

- 엔드유저 기능은 JWT subject와 wallet ownership 경계를 통과한다.
- 운영 조회 기능은 operator 또는 admin 권한을 요구한다.
- 운영 변경 기능은 admin token과 operator id를 요구한다.
- broker/internal endpoint는 shared secret 또는 동등한 인증을 요구한다.
- 배포 프로파일에서 기본 secret/token이 fail-fast로 차단된다.
- 새 endpoint는 admin path matcher, audit filter, security chain drift guard 대상인지 확인된다.

### 운영 관측성

- 실패하거나 지연될 수 있는 기능은 운영자가 확인할 상태, metric, health, alert, audit 중 하나 이상을 제공한다.
- warning/critical 성격의 상태는 operational alert 저장 또는 명시적 non-alert 사유를 가진다.
- 반복 실행되는 운영 작업은 보존 기간, pruning, 중복 억제, 실패 시 재시도 여부를 문서화한다.
- Slack webhook 같은 외부 push는 실패가 핵심 API를 실패시키지 않는지, 실패 기록/재시도 정책이 필요한지 판단한다.

### 테스트/CI

- 백엔드 변경은 최소 `./gradlew test`와 영향 범위에 따라 `./gradlew scenarioTest`, `./gradlew postgresScenarioTest`, `./gradlew check`를 통과한다.
- 프론트 변경은 `npm --prefix frontend run test`, `npm --prefix frontend run build`, 필요 시 `npm --prefix frontend run e2e`를 통과한다.
- 공개 후보는 다음 전체 gate를 통과한다.

```bash
./gradlew check
./gradlew scenarioTest
./gradlew postgresScenarioTest
npm --prefix frontend run test
npm --prefix frontend run build
npm --prefix frontend run e2e
scripts/mvp-local-smoke.sh
docker compose config
git diff --check
```

- Wiki publish가 release 조건이면 `scripts/sync-wiki-drafts.sh wiki-drafts <wiki-checkout>` 결과도 확인한다.
- 문서/운영 규칙 동기화가 필요한 변경이면 `scripts/check-dev-rules.sh`를 통과한다.

### 문서/ADR/progress/release note

- 결정 변경은 ADR에 남긴다.
- 작업 완료 결과, 검증, 남은 일은 `docs/progress/`에 남긴다.
- 공개 후보 범위는 `docs/releases/unreleased.md` 또는 버전 release note에 반영한다.
- 사용자 실행 절차나 테스트 절차가 바뀌면 README 또는 local test guide를 갱신한다.
- 알려진 제약은 release note에 숨기지 않고 "공개 조건" 또는 "후속 후보"로 분리한다.

### 배포/롤백

- 공개 후보는 tag 전 release PR CI가 모두 통과해야 한다.
- local smoke 실패 시 tag를 만들지 않는다.
- k8s 데모 범위면 `scripts/k8s-local-up.sh`와 관련 smoke/접속 문서가 현재 상태와 맞아야 한다.
- Wiki publish 실패 시 GitHub Release에 미반영 상태를 명시하거나 release를 보류한다.
- rollback은 새 tag 미발행, release note 보류, 기능 flag/스케줄러 기본 비활성 유지 중 하나로 설명 가능해야 한다.

## Release Go / No-Go 체크리스트

이 체크리스트는 GitHub Issue나 release checklist로 그대로 옮길 수 있다.

### Scope

- [ ] release 성격이 실제 금융 운영이 아니라 외부 시연/포트폴리오/로컬 또는 k8s 데모임을 명시했다.
- [ ] 포함 기능이 Must Have, Should Have, Defer, Out of Scope 중 하나로 분류됐다.
- [ ] 알려진 제약이 release note에 공개 조건 또는 후속 후보로 들어갔다.

### Product Flow

- [ ] 로그인 후 지갑 잔액 조회가 화면과 API에서 동작한다.
- [ ] 충전 성공 흐름이 화면과 API에서 동작한다.
- [ ] 송금 성공 흐름이 화면과 API에서 동작한다.
- [ ] 잔액 부족 또는 잘못된 요청 오류가 사용자가 이해할 수 있는 형태로 표시된다.
- [ ] 거래내역, 원장, 감사 로그 중 공개 시연에서 보여줄 증거가 정해졌다.

### Domain Integrity

- [ ] 성공한 충전/송금이 잔액, 거래내역, 원장, 감사 로그, step log, outbox event로 추적된다.
- [ ] Idempotency-Key 재시도와 conflict가 테스트된다.
- [ ] PostgreSQL row lock, lock timeout, 2-wallet lock 순서가 테스트된다.
- [ ] outbox claim/retry/lease/max-attempt/manual-review 흐름이 테스트된다.
- [ ] requeue approve/reject/execute 경합에서 단 하나의 상태 전이만 성공한다.

### Security

- [ ] 엔드유저 JWT와 wallet ownership 검사가 지갑 조회/명령에 적용된다.
- [ ] 운영 조회 API는 operator 또는 admin token을 요구한다.
- [ ] 운영 변경 API는 admin token과 operator id를 요구한다.
- [ ] direct requeue API는 `410 Gone`으로 실패한다.
- [ ] `/internal/broker/outbox-events`는 `X-Broker-Token` 없이 401을 반환한다.
- [ ] 배포 프로파일에서 기본 JWT secret, 운영 token, broker token이 fail-fast로 차단된다.
- [ ] 운영 API path matcher와 security chain drift guard가 새 경로를 포함한다.

### Operations

- [ ] relay run과 relay health를 조회할 수 있다.
- [ ] consumer metrics, window metrics, health, receipts를 조회할 수 있다.
- [ ] warning/critical operational alert가 record로 저장된다.
- [ ] operational alert suppression과 pruning 기준이 문서화됐다.
- [ ] admin API access audit을 조회할 수 있다.
- [ ] operational log pruning과 consumer pruning을 수동 실행할 수 있다.
- [ ] Slack webhook 사용 여부와 실패 처리 제약이 release note에 들어갔다.

### Verification

- [ ] `./gradlew check` 통과.
- [ ] `./gradlew scenarioTest` 통과.
- [ ] `./gradlew postgresScenarioTest` 통과.
- [ ] `npm --prefix frontend run test` 통과.
- [ ] `npm --prefix frontend run build` 통과.
- [ ] `npm --prefix frontend run e2e` 통과.
- [ ] `scripts/mvp-local-smoke.sh` 통과.
- [ ] `docker compose config` 통과.
- [ ] `git diff --check` 통과.
- [ ] 필요한 경우 `scripts/check-dev-rules.sh` 통과.
- [ ] 필요한 경우 `scripts/sync-wiki-drafts.sh wiki-drafts <wiki-checkout>` 결과 확인.

### Release

- [ ] Release PR CI가 모두 통과했다.
- [ ] `docs/releases/unreleased.md`가 실제 버전 release note로 승격됐다.
- [ ] build version, release note, progress index, wiki draft가 같은 버전을 가리킨다.
- [ ] GitHub Release body에 포함 기능, 검증 결과, 알려진 제약, 다음 후보가 들어갔다.
- [ ] local smoke 실패, CI 실패, Wiki publish 실패 시 tag를 발행하지 않는다는 rollback 기준이 확인됐다.

## 다음 개선 후보 우선순위

### P0: 실오픈 blocker

아래 항목은 공개 범위에 포함한다고 결정한 경우 blocker다. 포함하지 않는다면 release note의 알려진 제약으로 내려야 한다.

- 운영자 화면에서 relay health, pruning, operational alert를 보여주겠다고 약속한 경우 해당 화면과 E2E 증거.
- k8s 데모를 공개 범위로 잡은 경우 `scripts/k8s-local-up.sh` 성공, 접속 URL 확인, smoke 증거.
- broker consumer 인증을 공개 범위로 잡은 경우 미인증 401, publisher secret 부착, 배포 프로파일 fail-fast 검증.
- GitHub Wiki 공개를 release 조건으로 잡은 경우 Wiki publish와 핵심 페이지 확인.
- release note가 실제 구현보다 강한 보안/운영 수준을 암시하는 문구 제거.

### P1: 공개 품질 향상

- Slack 발행 실패 record와 재시도 정책.
- pruning 실행 이력 저장과 조회 API.
- 운영 alert 화면 연결.
- 실제 identity/role scope 연동 또는 최소한 operator id 위조 가능성에 대한 명확한 공개 제약.
- broker replay window별 retention 권장값 문서화.
- frontend E2E에서 운영 관측 화면 증거 강화.

### P2: 후속 학습/확장

- Kafka/RabbitMQ/SQS adapter와 broker-specific Testcontainers contract.
- JWT refresh 정책.
- broker 장애 주입과 복구 시나리오.
- ELK 스택을 release gate가 아닌 학습 모듈로 정리.
- `App.tsx` 컴포넌트 분해.
- 원장 append-only DB 제약, 실패 감사 정책, house account 같은 도메인 심화.

## 최종 판정

현재 문서 기준으로 이 프로젝트는 실제 금융 운영 배포는 불가능하고, 그렇게 공개해서도 안 된다. 다만 외부 시연, 포트폴리오, 로컬/k8s 데모 기준의 MVP 공개는 조건부로 가능하다.

조건은 다음과 같다.

- 공개 설명에서 학습용 핀테크 엔지니어링 랩임을 명시한다.
- 실제 금융 거래, 실명 인증, 외부 결제망, 개인정보 처리는 범위 밖이라고 명시한다.
- release gate 명령과 local smoke 결과를 release PR 또는 GitHub Release body에 첨부한다.
- operator identity가 헤더 기반이라는 제약, broker-specific adapter 부재, Slack 실패 재시도 부재, 실제 IAM 미연동을 알려진 제약으로 공개한다.
- v0.7.0 이후 `unreleased` 후보 중 공개 범위에 넣을 항목만 Must/Should로 승격하고, 나머지는 Defer로 남긴다.

판정: **GO with constraints**. MVP 공개는 가능하지만, 공개 문구와 release note가 "실제 금융 운영 가능"으로 읽히는 순간 No-Go다.
