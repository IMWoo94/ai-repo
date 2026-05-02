# Local Setup

이 문서는 IntelliJ IDEA에서 `ai-repo` Spring Boot 프로젝트를 실행하기 위한 로컬 기준을 설명합니다.

## 기준 버전

ADR-0003 기준:

- Java 25
- Spring Boot 4.x
- Gradle 9.3.0 Wrapper

## IntelliJ 설정

1. Java 25 SDK를 설치합니다.
2. IntelliJ에서 `File > Project Structure > Project SDK`를 Java 25로 설정합니다.
3. `build.gradle`을 Gradle 프로젝트로 import합니다.
4. Gradle JVM을 Java 25로 설정합니다.
5. IntelliJ가 사용할 Gradle은 이 저장소의 Gradle Wrapper를 사용합니다.
6. 테스트는 Gradle 또는 IntelliJ JUnit runner로 실행합니다.

## 검증 명령

다음 명령을 기준으로 사용합니다.

```bash
./gradlew test
./gradlew scenarioTest
./gradlew check
```

## 실행 프로필

기본 실행은 인메모리 저장소를 사용합니다.

```bash
./gradlew bootRun
```

PostgreSQL 저장소는 `postgres` 프로필에서 활성화합니다.

로컬 PostgreSQL 실행:

```bash
docker compose up -d postgres
docker compose ps postgres
```

애플리케이션 실행:

```bash
AI_REPO_POSTGRES_URL=jdbc:postgresql://localhost:5432/ai_repo \
AI_REPO_POSTGRES_USERNAME=ai_repo \
AI_REPO_POSTGRES_PASSWORD=ai_repo \
SPRING_PROFILES_ACTIVE=postgres \
./gradlew bootRun
```

`postgres` 프로필은 Flyway를 활성화하고 `src/main/resources/db/migration`의 migration을 실행합니다.

현재 migration 기준:

- `V1__create_wallet_schema.sql`: 회원, 지갑, 잔액, 거래내역, 멱등키, 원장, 감사 로그 스키마
- `V2__seed_wallet_fixture.sql`: 학습용 회원, 지갑, 잔액, 거래내역 샘플 데이터
- `V3__create_operation_step_logs.sql`: 충전/송금 논리적 트랜잭션 단계 로그 스키마
- `V4__create_operation_outbox_events.sql`: 충전/송금 성공 이벤트 outbox 적재 스키마
- `V5__add_outbox_relay_state.sql`: outbox relay 상태 전이와 재시도 메타데이터 스키마
- `V6__add_outbox_retry_schedule.sql`: outbox retry schedule 스키마
- `V7__add_outbox_processing_lease.sql`: outbox processing lease 스키마
- `V8__create_outbox_requeue_audits.sql`: outbox requeue 감사 이력 스키마

`src/main/resources/db/postgresql/schema.sql`과 `src/main/resources/db/postgresql/fixtures.sql`은 H2 기반 빠른 저장소 테스트와 수동 비교를 위해 일시적으로 유지합니다. PostgreSQL 프로필의 기준은 Flyway migration입니다.

스키마 변경 규칙:

- 이미 적용된 `V*__*.sql` migration은 수정하지 않습니다.
- 스키마 변경은 새 버전 migration으로 추가합니다.
- 샘플 데이터 정책 변경은 운영 데이터 정책이 아니라 개발/검증 seed 정책으로 분리해서 기록합니다.

로컬 PostgreSQL 정리:

```bash
docker compose down
```

## 현재 제약

Gradle Wrapper는 `9.3.0`을 사용합니다. Java 25 SDK가 설치되어 있지 않으면 toolchain 해석 또는 컴파일 단계에서 실패할 수 있습니다.

PostgreSQL 저장소는 H2 PostgreSQL mode 기반 테스트와 Testcontainers 기반 실제 PostgreSQL 테스트로 검증합니다. Testcontainers 테스트는 Flyway migration 경로를 실행해 PostgreSQL 런타임 스키마를 준비합니다. Docker가 없는 로컬 환경에서는 Testcontainers 테스트가 스킵될 수 있습니다.

Outbox relay scheduler는 기본 비활성화입니다. 로컬에서 자동 relay 실행을 확인하려면 다음 환경 변수를 설정한 뒤 애플리케이션을 실행합니다.

```bash
AI_REPO_OUTBOX_RELAY_SCHEDULER_ENABLED=true \
AI_REPO_OUTBOX_RELAY_BATCH_SIZE=10 \
AI_REPO_OUTBOX_RELAY_INITIAL_DELAY_MS=5000 \
AI_REPO_OUTBOX_RELAY_FIXED_DELAY_MS=5000 \
./gradlew bootRun
```

수동 검증 중 잔액/거래/outbox 상태가 자동으로 바뀌면 scheduler가 켜져 있는지 먼저 확인합니다.

## 의존성 관리

Spring Boot Gradle plugin만 적용하면 버전 없는 starter 의존성의 버전 관리가 자동으로 적용되지 않을 수 있습니다. 이 프로젝트는 Spring Boot 공식 Gradle 문서의 dependency management plugin 방식을 사용합니다.

- `org.springframework.boot` plugin: Boot 실행/패키징 지원
- `io.spring.dependency-management` plugin: Spring Boot BOM 기반 의존성 버전 관리
