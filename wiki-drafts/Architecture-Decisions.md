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

## 중요한 트레이드오프

| 결정 | 선택 | 포기한 것 |
| --- | --- | --- |
| 모놀리식 우선 | 한 프로세스에서 도메인과 운영 흐름을 빠르게 검증 | 초기부터 MSA를 도입해 실제 분산 실패를 재현하는 범위 |
| 인메모리 기본 실행 | 로컬 시연과 프론트 E2E 속도 확보 | 기본 실행에서 영속 데이터 유지 |
| PostgreSQL profile 분리 | 운영 유사 검증을 명시적으로 실행 | 모든 로컬 실행을 DB 필수로 만드는 단순성 |
| Transactional outbox | 돈 이동과 event 적재 정합성 확보 | 즉시 Kafka/RabbitMQ/SQS에 결합 |
| Header 기반 운영 인증 | local MVP에서 운영 API 보호 계약 고정 | 실제 OAuth/OIDC 로그인 |
| Wiki는 요약, ADR은 결정 | 포트폴리오 설명성과 PR 검증성 모두 확보 | Wiki 하나에 모든 결정을 몰아넣는 단순성 |

## 다음 구조 후보

- release smoke script 또는 actuator health endpoint
- relay health/pruning 운영자 화면
- manual review requeue full E2E fixture
- broker-specific adapter와 Testcontainers contract
- consumer idempotency
- 실제 운영자 identity와 role scope 분리
