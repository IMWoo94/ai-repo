# Architecture Decisions

이 문서는 ADR 목록을 비기술 독자도 따라갈 수 있도록 결정 지도로 요약한 Wiki 초안이다.

결정의 source of truth는 `docs/adr/`이다. 이 문서는 ADR을 대체하지 않고 읽기 순서와 맥락을 제공한다.

## 현재 구조 요약

| 영역 | 현재 선택 | 이유 |
| --- | --- | --- |
| 애플리케이션 | Java 25, Spring Boot, Gradle 모놀리식 | 학습과 검증 속도를 우선하고, 추후 MSA 전환 전 도메인 경계를 먼저 고정한다 |
| 저장소 | 기본 인메모리, `postgres` profile JDBC/PostgreSQL | 빠른 로컬 실행과 운영 유사 검증을 둘 다 유지한다 |
| 스키마 | Flyway migration | DB 변경 이력을 릴리스와 함께 추적한다 |
| 돈 이동 정합성 | 원장, 감사 로그, operation step log | 잔액 변경과 단계별 처리 흔적을 분리해 설명 가능성을 높인다 |
| 외부 반응 | Transactional outbox와 relay | 송금 성공과 event 발행 후보를 같은 트랜잭션에 묶는다 |
| 운영 조치 | manual review, requeue, audit trail | 자동 처리 실패를 운영자 책임 추적 가능한 흐름으로 분리한다 |
| 운영 API 보안 | Spring Security role model, header 기반 local token | 실제 로그인 전에도 운영 API 노출 위험을 낮춘다 |
| 프론트 | React, TypeScript, Vite, Playwright/Vitest | 로컬 시연과 자동 회귀 검증을 함께 확보한다 |

## ADR 읽기 순서

1. 문서 책임과 테스트 전략
   - ADR-0001 Documentation Source of Truth
   - ADR-0002 Test Strategy
2. Java/Spring Boot 기반
   - ADR-0003 Java Spring Boot Gradle Baseline
3. 지갑과 돈 이동
   - ADR-0005 Member Wallet Account Query Policy
   - ADR-0006 Charge Transfer Idempotency Policy
   - ADR-0007 Ledger Audit Log Boundary
4. PostgreSQL과 동시성
   - ADR-0008 PostgreSQL Persistence Profile
   - ADR-0009 PostgreSQL Runtime Verification
   - ADR-0010 Flyway Schema Migrations
   - ADR-0011 PostgreSQL Balance Row Locking
   - ADR-0012 PostgreSQL Lock Timeout Policy
5. 운영 관측과 outbox
   - ADR-0013 Operation Step Log Before Outbox Saga
   - ADR-0014 Transactional Outbox Boundary
   - ADR-0015 Outbox Relay State
   - ADR-0016 Outbox Claiming Retry Policy
   - ADR-0017 Outbox Processing Lease Recovery
   - ADR-0018 Outbox Max Attempt Manual Review
   - ADR-0019 Outbox Manual Review API
   - ADR-0020 Outbox Requeue Audit Trail
6. 릴리스와 검증
   - ADR-0021 Release Version Baseline
   - ADR-0022 Scenario Based Test Pipeline
   - ADR-0025 Frontend E2E Test Pipeline
   - ADR-0026 Frontend Component Test Pipeline
   - ADR-0036 PostgreSQL Scenario Testcontainers CI
7. 운영 API와 broker 경계
   - ADR-0027 Outbox Publisher Port
   - ADR-0028 Admin API Authz
   - ADR-0029 Outbox Relay Scheduler
   - ADR-0030 Outbox Relay Run Monitoring
   - ADR-0031 Admin API Access Audit
   - ADR-0032 Operational Log Pruning
   - ADR-0033 Outbox Relay Health Metrics Alert
   - ADR-0034 Spring Security Role Model
   - ADR-0035 HTTP Outbox Broker Adapter
   - ADR-0037 Operator/Admin Token Split
   - ADR-0038 Requeue Approval Workflow
   - ADR-0039 Dev Rules Automatic Sync Check
   - ADR-0040 Direct Requeue API Deprecation
   - ADR-0041 Requeue State Transition Atomicity
   - ADR-0042 Outbox Claim Guarded Result Update
   - ADR-0043 Broker and Consumer Idempotency Contract
   - ADR-0044 Consumer Processed Event Dedupe Store
   - ADR-0045 HTTP Outbox Consumer Adapter
   - ADR-0046 HTTP Publish Consume Loop Verification
   - ADR-0047 Consumer Monitoring Admin API
   - ADR-0048 Consumer Processed Event Pruning
   - ADR-0049 Consumer Duplicate Spike Alert
   - ADR-0050 Consumer Duplicate Time Bucket Metric
   - ADR-0051 Consumer Delivery Metric Pruning
   - ADR-0052 Operational Alert Record Channel
   - ADR-0053 Operational Alert Suppression and Pruning
   - ADR-0054 Slack Webhook Operational Alert Publisher
   - ADR-0055 Admin API Path Matching Hardening
   - ADR-0056 End-User JWT Auth and Wallet Ownership
   - ADR-0057 End-User Login and Bearer Refresh
   - ADR-0065 Broker Consumer Endpoint Authentication
8. 구조 경계와 persistence adapter 분해
   - ADR-0063 Application Layer Spring Annotation Policy
   - ADR-0064 JDBC Persistence Adapter Decomposition
9. 로깅 관측 (opt-in)
   - ADR-0066 ELK Logging Stack (opt-in, PLG와 병존)

## 중요한 트레이드오프

| 결정 | 선택 | 포기한 것 |
| --- | --- | --- |
| 모놀리식 우선 | 한 프로세스에서 도메인과 운영 흐름을 빠르게 검증 | 초기부터 MSA를 도입해 실제 분산 실패를 재현하는 범위 |
| 인메모리 기본 실행 | 로컬 시연과 프론트 E2E 속도 확보 | 기본 실행에서 영속 데이터 유지 |
| PostgreSQL profile 분리 | 운영 유사 검증을 명시적으로 실행 | 모든 로컬 실행을 DB 필수로 만드는 단순성 |
| Transactional outbox | 돈 이동과 event 적재 정합성 확보 | 즉시 Kafka/RabbitMQ/SQS에 결합 |
| Header 기반 운영 인증 | local MVP에서 운영 API 보호 계약 고정 | 실제 OAuth/OIDC 로그인 |
| 직접 requeue API 비활성화 | 승인 workflow 우회 방지 | 단일 API로 빠르게 재처리하는 편의성 |
| Requeue row lock과 update count 검증 | 동시 운영 조치에서 하나의 상태 전이만 허용 | 상태 전이마다 짧은 DB row lock 비용 |
| Outbox claim 기반 결과 갱신 | 늦은 worker가 재claim된 event 상태를 덮지 못하게 함 | worker identity 없는 최소 방어라 외부 broker 중복 발행은 별도 과제 |
| Broker/consumer idempotency 계약 | `outboxEventId`를 header/body idempotency key로 고정 | broker-specific consumer adapter는 후속 과제 |
| Consumer processed-event 저장소 | `idempotencyKey` unique 제약으로 duplicate side effect 방지 기반 확보 | broker별 replay window 권장값은 후속 과제 |
| HTTP consumer adapter | 실제 endpoint에서 duplicate를 성공 no-op으로 처리하고 receipt side effect를 1회만 저장 | Kafka/RabbitMQ/SQS ack/nack 모델은 후속 과제 |
| HTTP publish→consume loop | relay가 실제 HTTP publisher로 consumer endpoint에 보내는 흐름 검증 | durable broker semantics는 후속 과제 |
| Consumer monitoring admin API | 처리 성공, duplicate no-op, receipt를 운영자가 조회 | duplicate payload 상세 이력은 후속 과제 |
| Consumer processed-event pruning | dedupe/receipt 저장소를 보존 기간 기준으로 정리 | broker replay window보다 짧은 retention은 위험하므로 운영 설정 책임이 남음 |
| Consumer duplicate spike alert | duplicate delivery rate 기준으로 health를 판정 | push alert는 후속 과제 |
| Consumer duplicate time bucket metric | 최근 window duplicate delivery rate를 분 단위 bucket으로 계산 | 초 단위 spike 정밀도 |
| Consumer delivery metric pruning | delivery metric bucket을 기존 consumer pruning run에서 함께 정리 | pruning run 이력 저장은 후속 과제 |
| Operational alert record channel | warning/critical health를 운영 alert record로 저장 | Slack/Webhook push는 후속 과제 |
| Operational alert suppression/pruning | 같은 alert의 짧은 시간 중복 저장을 막고 보존 기간 기준으로 삭제 | suppression 기준이 reason 문자열에 의존 |
| Slack webhook operational alert publisher | Incoming Webhook 호환 text payload로 push channel 계약 고정 | 발행 실패 record와 재시도 정책은 후속 과제 |
| Admin API path matching hardening | 인증·감사 필터가 공통 segment-aware matcher로 root/sub-path만 운영 API로 분류 | Spring Security matcher 목록과 완전한 단일 source of truth는 후속 과제 |
| Audit event–wallet 매핑 명시화 | `audit_event_wallets` 매핑 테이블을 쓰기 시점에 영속화(charge 1행/transfer 2행)해 소유자 스코프 audit-events 조회의 ledger 조인 의존을 제거 | 쓰기 경로에 매핑 삽입 1~2행 추가와 정합성 책임 |
| Broker consumer endpoint 인증 (ADR-0065) | `/internal/broker/**` 전용 SecurityFilterChain(Order 3) + `X-Broker-Token` 상수시간 비교로 미인증 outbox event 주입 차단, publisher가 같은 secret 부착, admin chain의 filter/error-handler 축 재사용 | shared secret은 NetworkPolicy 전제 위 2차 방어선이며, 단일 토큰이라 로테이션은 순단 수용(다중 토큰은 후속) |
| Wiki는 요약, ADR은 결정 | 포트폴리오 설명성과 PR 검증성 모두 확보 | Wiki 하나에 모든 결정을 몰아넣는 단순성 |
| 로컬 k8s 배포와 관측 스택 (ADR-0059) | Docker Desktop k8s + kustomize + Prometheus/Grafana/Loki, GitHub Actions→GHCR→ArgoCD GitOps, deploy는 CI 성공 게이트(workflow_run) 뒤에서만 실행 | 익명 Grafana/평문 자격증명은 로컬 학습 전용, 원격 전환 시 재설계 |
| k8s 자격증명 Secret 주입 (ADR-0061) | `deploy/k8s` 평문 env(DB/운영 토큰/JWT)를 `Secret ai-repo-credentials`의 `secretKeyRef`로 전환 | 로컬 고정값 Secret도 평문 커밋 — 목적은 비밀 은닉이 아니라 스테이징 사고 방지+원격 전환 준비 |
| 배포 프로파일 기본 자격증명 fail-fast (ADR-0062) | `postgres`/`prod`에서 공개된 기본 JWT secret·운영 토큰이면 기동 실패, `deploy/k8s/app.yaml`에 명시 값 주입 | 배포 프로파일 목록 하드코딩과 `app.yaml` 평문 값(Secret 주입은 #121에서 완결) |
| Application layer Spring annotation policy (ADR-0063) | `@Service`, policy/properties `@Component`/`@Value`, 필요한 `@Transactional`을 제한적으로 허용하고 adapter 의존은 금지 | framework-free usecase 분리는 후속 전환 ADR 필요 |
| JDBC persistence adapter decomposition (ADR-0064) | `JdbcWalletRepository`는 Spring composite bean으로 유지하고 SQL은 wallet/ledger, outbox relay, outbox consumer, operational alert, admin audit adapter로 분리 | composite가 여러 port를 계속 구현하는 절충은 유지 |
| opt-in ELK 로그 스택 (ADR-0066) | 학습용 ELK(Filebeat→Logstash grok→Elasticsearch 역색인→Kibana)를 `logging` 네임스페이스에 PLG와 **병존**시키되 ArgoCD 미포함·수동 스크립트로만 기동, 2단계(Logstash grok) 토폴로지로 파싱 학습 | 로컬 학습 전용(security off·단일노드·ephemeral)이라 운영 신뢰성은 범위 밖, PLG 대비 리소스 비용이 크고 상시 구동하지 않음 |

## 다음 구조 후보

- Spring Security matcher와 운영 API matcher 목록을 단일 source of truth 상수로 통합(현재는 `OperationalApiPathDriftGuardTest`가 두 목록 동기화를 검증)
- broker-specific adapter와 Testcontainers contract
- broker replay window별 retention 권장값
- pruning 실행 이력 저장과 조회 API
- Slack 발행 실패 record와 재시도 정책
- 실제 운영자 identity와 role scope 분리
- context별 JDBC adapter slice test 분리
