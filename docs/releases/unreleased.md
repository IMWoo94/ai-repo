# Unreleased Release Candidate Notes

## 릴리스 성격

`unreleased`는 `v0.7.0` 이후 `main`에 누적될 변경 후보를 추적한다.

이 문서는 GitHub Release가 아니라, 다음 실제 tag를 만들기 전 현재 개발분을 정리하는 staging 문서다.

## 후보 범위

- Manual review requeue 성공 E2E fixture
- 운영자 relay health/pruning 화면
- operator/admin token 분리와 role model 강화
- Requeue 승인 워크플로우
- Requeue 반려 워크플로우
- 직접 requeue API deprecation
- Requeue 상태 전이 원자화
- Outbox stale writer 방지
- Broker/consumer idempotency 계약
- Consumer processed-event dedupe 저장소
- HTTP outbox consumer adapter와 duplicate side effect 1회 검증
- HTTP publish→consume loop 검증
- Consumer monitoring admin API
- Consumer processed-event pruning
- Consumer duplicate spike alert
- Consumer duplicate time bucket metric
- Consumer delivery metric pruning
- Operational alert record channel
- Operational alert suppression and pruning
- Slack webhook operational alert publisher
- `.dev/rules` 자동 문서/테스트 동기화 체크
- Admin API path matching hardening
- 엔드유저 memberId JWT 인증과 wallet ownership 서비스계층 강제(IDOR 차단, 비소유자 403, ledger-entries 포함)
- 프론트엔드 memberId 로그인 + Bearer 토큰 부착 + 401 자동 재발급/재시도
- audit-events/step-logs/outbox-events 미인증 노출 차단(operator 게이팅)과 소유자 스코프 `GET /wallets/{walletId}/audit-events` 추가
- 사용자 흐름 e2e 증거 단언을 원장+감사 로그 기준으로 교체(step log/outbox 패널 제거 후속)
- 로컬 k8s 배포 스택(Prometheus/Grafana/Loki)과 outbox 커스텀 지표
- GitHub Actions → GHCR → GitOps → ArgoCD 배포 파이프라인
- 소유자 스코프 audit-events의 ledger 조인 의존 제거 — `audit_event_wallets` 매핑 테이블을 쓰기 시점에 영속화(charge 1행/transfer 2행)하고 조회를 매핑 기반으로 전환(V18 역채움 포함)
- k8s 로컬 스크립트 하드닝 — `k8s-local-up.sh`가 loki/alloy rollout까지 대기 + Loki `/ready` 스모크, `argocd-install.sh` 설치 매니페스트 버전 고정(v3.4.4)
- `deploy/k8s` 평문 자격증명 env(DB/운영 토큰/JWT)를 `Secret ai-repo-credentials`의 `secretKeyRef`로 전환(로컬 고정값, 원격 별도 주입)
- 운영 API 경로 목록 drift 방지 테스트 — `SecurityConfig`와 `AdminApiPathMatcher` 두 상수 목록이 같은 운영 API root 집합을 다루는지 양방향 검증(불일치 시 누락 경로 명시)
- 배포 프로파일(`postgres`/`prod`)에서 공개된 기본 JWT secret·운영 토큰 fail-fast 확장(`JwtSecretGuard`/`OpsTokenGuard`)과 `deploy/k8s/app.yaml` 명시 값 주입
- JDBC persistence adapter 분해 — `JdbcWalletRepository`를 PostgreSQL profile composite bean으로 유지하면서 wallet/ledger, outbox relay, outbox consumer, operational alert, admin audit SQL을 context별 package-private adapter로 분리하고 ArchUnit 레이어 규칙을 추가
- `POST /internal/broker/outbox-events` shared secret 헤더(`X-Broker-Token`) 인증 — 전용 SecurityFilterChain(Order 3) + 상수시간 토큰 비교로 미인증 event 주입 차단, publisher가 같은 secret 부착(#123)
- opt-in ELK 로깅 스택 — 학습용 Filebeat→Logstash(grok)→Elasticsearch(역색인)→Kibana를 `logging` 네임스페이스에 PLG(Loki)와 병존시키되 ArgoCD 미포함·`scripts/elk-local-up.sh`/`down.sh` 수동 기동, 로컬 학습 전제(security off·단일노드·ephemeral), Spring 로그 grok 파싱과 `spring-logs-*` KQL 검색. `AI_REPO_ELK_ENABLED` 토글·독립 스크립트·별도 ArgoCD Application(수동 sync)로 on/off (ADR-0066)
- 배포 outbox publisher 배선 — `deploy/k8s/app.yaml`이 `AI_REPO_OUTBOX_PUBLISHER_TYPE`를 안 줘 memory 기본값으로 event가 유실되던 결함을 `type=http` + 자기 Service endpoint(`http://ai-repo:8080/internal/broker/outbox-events`) 주입으로 수정하고, http 기본 endpoint 경로를 컨슈머 매핑(`/internal/broker/outbox-events`)과 정합 (ADR-0067, #144)
- 배포 프로파일 영속성 fail-fast 가드 — `DeployedProfilePersistenceGuard`가 배포 프로파일(`prod`/`postgres`)인데 `postgres`가 없으면 startup을 실패시켜, `prod` 단독 기동이 In-Memory 지갑 저장소를 로드해 재시작 시 잔액이 유실되는 경로를 막음(#145, ADR-0068)
- 송금 멱등 조회를 잔액 검사보다 먼저 수행 — 전액 송금으로 잔액이 0이 된 뒤 동일 `idempotencyKey` 재시도가 `InsufficientBalanceException`(422)으로 거부되던 회귀 수정. `transfer`에서 fingerprint 계산 후 멱등 조회를 먼저 하고 없을 때만 잔액 검사·`applyTransfer`, conflict(409) 감지는 유지, `charge`와 순서 일관화 (#146, ADR-0069)
- 핫 쿼리 인덱스 추가 + outbox claim 배치 업데이트 — `ledger_entries(wallet_id, occurred_at)`·`transaction_history(wallet_id, occurred_at)`·`operation_outbox_events(status, occurred_at, outbox_event_id)` 인덱스를 V19 마이그레이션과 통합 스키마에 추가하고, relay claim의 행 단위 UPDATE 루프를 `where outbox_event_id in (...)` 단일 배치로 교체(의미·SKIP LOCKED 선행 claim·트랜잭션 경계 보존, PUBLISHED pruning은 후속) (ADR-0071, #148)
- 멱등키를 지갑 스코프로 한정 — `wallet_operations` PK를 `(idempotency_key)`에서 `(wallet_id, idempotency_key)` 복합 PK로 전환(V20)하고 `findOperation(walletId, idempotencyKey)`로 조회를 지갑 스코프화해 회원 간 멱등키 충돌(409 DoS 가능성)과 cross-tenant operation 재생을 차단 (ADR-0072, #149)
- 미검증 입력/응답 경계 하드닝 — charge/transfer description 255자 초과를 400(`INVALID_WALLET_OPERATION`)으로 차단, 지갑 미인증 요청에 `WALLET_AUTHENTICATION_REQUIRED` JSON 401(`WalletSecurityErrorHandler`) 반환, outbox 실패 기록 `last_error`를 255자로 절단, 프론트가 빈 401 본문을 `SyntaxError` 대신 인증 오류로 처리(ADR-0070, #147)

## MVP 출시 판단 기준

다음 release 후보는 다음 조건을 만족해야 한다.

- 핵심 사용자 흐름인 잔액 조회, 충전, 송금, 거래내역 확인이 화면과 API에서 동작한다.
- 돈 이동 결과가 원장, 감사 로그, operation step log, outbox event로 추적된다.
- 운영자는 manual review outbox, requeue 요청/승인/실행/반려, relay health, relay run, pruning 결과를 화면에서 확인할 수 있다.
- 직접 requeue API는 workflow 우회를 막기 위해 `410 Gone`으로 실패한다.
- Requeue approve/reject/execute 경합은 PostgreSQL Testcontainers에서 하나의 전이만 성공하는지 검증한다.
- Outbox publish 결과 갱신은 claim lease가 일치할 때만 성공해 늦은 worker가 재claim 상태를 덮지 못한다.
- Broker publish envelope는 schema version과 idempotency key를 포함하고, consumer는 같은 key를 unique 처리 기준으로 삼는다.
- Consumer processed-event 저장소는 같은 `idempotencyKey`의 중복 기록을 막고 PostgreSQL 동시성 테스트로 검증된다.
- HTTP consumer endpoint는 duplicate event를 성공 no-op으로 처리하고 receipt side effect를 한 번만 저장한다.
- Outbox relay는 실제 HTTP publisher로 local consumer endpoint에 event를 보내고 receipt와 `PUBLISHED` 상태를 남기는 loop test를 가진다.
- 운영자는 consumer processed count, duplicate count, receipt count와 최근 receipt를 조회할 수 있다.
- 운영자는 오래된 consumer processed-event, receipt, delivery metric bucket을 보존 기간 기준으로 pruning할 수 있다.
- 운영자는 consumer duplicate delivery rate 기준의 `OK`, `WARNING`, `CRITICAL`, `NO_DATA` health를 최근 window 기준으로 조회할 수 있다.
- 같은 operational alert는 suppression window 안에서 중복 저장되지 않고, 오래된 alert record는 operational log pruning에서 삭제된다.
- 설정 시 warning/critical operational alert는 Slack Incoming Webhook 호환 endpoint로 push된다.
- CI는 `.dev/rules` 기반 문서, 테스트, Wiki 동기화 누락 검사를 수행한다.
- 운영 API는 local operator/admin token과 operator id로 보호된다. 인증 필터와 접근 감사 필터는 같은 segment-aware 운영 API path matcher를 사용하므로 root/sub-path만 보호·감사하고 lookalike prefix는 오탐하지 않는다.
- PostgreSQL profile이 Flyway migration과 Testcontainers scenario로 검증된다.
- 프론트 build, unit, E2E가 CI에서 분리 검증된다.
- 백엔드 unit/API, scenario, PostgreSQL scenario가 CI에서 분리 검증된다.
- README, ADR, progress, issue draft, local test guide가 현재 상태와 모순되지 않는다.
- JDBC persistence adapter는 bounded context별 adapter로 분해되어도 기존 repository contract와 레이어 의존 규칙을 통과한다.

## 검증 게이트

릴리스 후보 PR 또는 tag 전에는 다음 명령을 통과해야 한다.

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

GitHub Actions에서는 다음 job이 통과해야 한다.

- `Gradle Check`
- `Scenario Test`
- `PostgreSQL Scenario Test`
- `Frontend Unit Test`
- `Frontend Build`
- `Frontend E2E`

## 알려진 제약

- Slack webhook 발행 실패 record와 재시도 정책은 아직 없다.
- token과 operator identity는 local header 기반이며 실제 로그인과 분리되어 있다.
- Kafka/RabbitMQ/SQS 같은 broker-specific adapter는 아직 없다.
- GitHub Wiki 동기화는 아직 수동 후보 문서 수준이다.

## 출시 전 blocker

- 다음 release 범위가 확정되면 버전 번호와 tag 정책을 결정한다.
- `unreleased` 내용을 실제 버전 릴리스 노트로 승격한다.
- `scripts/mvp-local-smoke.sh` 실행 결과를 릴리스 PR에 첨부한다.

## 후속 후보

- broker-specific Testcontainers contract
- broker replay window별 retention 권장값
- pruning 실행 이력 저장과 조회 API
- Slack 발행 실패 record와 재시도 정책
- 실제 identity/role scope 연동
- 운영 alert 화면 연결
