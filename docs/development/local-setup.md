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

`postgres` 프로필은 `src/main/resources/db/postgresql/schema.sql`과 `src/main/resources/db/postgresql/fixtures.sql`을 실행합니다. 이번 단계에서는 Flyway/Liquibase를 도입하지 않았으므로 스키마 변경 이력 관리는 후속 작업으로 남깁니다.

로컬 PostgreSQL 정리:

```bash
docker compose down
```

## 현재 제약

Gradle Wrapper는 `9.3.0`을 사용합니다. Java 25 SDK가 설치되어 있지 않으면 toolchain 해석 또는 컴파일 단계에서 실패할 수 있습니다.

PostgreSQL 저장소는 H2 PostgreSQL mode 기반 테스트와 Testcontainers 기반 실제 PostgreSQL 테스트로 검증합니다. Docker가 없는 로컬 환경에서는 Testcontainers 테스트가 스킵될 수 있습니다.

## 의존성 관리

Spring Boot Gradle plugin만 적용하면 버전 없는 starter 의존성의 버전 관리가 자동으로 적용되지 않을 수 있습니다. 이 프로젝트는 Spring Boot 공식 Gradle 문서의 dependency management plugin 방식을 사용합니다.

- `org.springframework.boot` plugin: Boot 실행/패키징 지원
- `io.spring.dependency-management` plugin: Spring Boot BOM 기반 의존성 버전 관리
