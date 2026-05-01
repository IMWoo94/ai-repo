# ADR-0010: Flyway Schema Migrations

## 상태

Accepted

## 맥락

ADR-0008과 ADR-0009에서 PostgreSQL 저장소, 실제 PostgreSQL 검증, Docker Compose 로컬 실행 환경을 추가했다. 하지만 `postgres` 프로필은 여전히 Spring SQL init으로 `schema.sql`과 `fixtures.sql`을 실행한다.

Spring SQL init은 초기 샘플에는 충분하지만 스키마 변경 이력, 적용 순서, 운영 DB와 코드의 동기화를 설명하기 어렵다. 핀테크 학습 하네스에서는 원장, 감사 로그, 멱등키 테이블 변경 근거를 PR과 릴리스 단위로 추적할 수 있어야 한다.

## 선택지

### 선택지 A: Spring SQL init 유지

장점:

- 설정이 단순하다.
- 기존 SQL 파일과 테스트를 거의 유지할 수 있다.
- 초기 학습 속도가 빠르다.

단점:

- 스키마 변경 순서와 적용 이력을 추적하기 어렵다.
- 운영 DB에 이미 적용된 SQL을 수정하는 위험을 막기 어렵다.
- 릴리스 노트에서 마이그레이션 필요 여부를 증거로 남기기 어렵다.

### 선택지 B: Flyway 도입

장점:

- 현재 SQL-first 스키마 작성 방식과 잘 맞는다.
- `V1`, `V2`처럼 변경 순서가 파일명에 남는다.
- Spring Boot의 자동 구성과 Testcontainers 검증에 연결하기 쉽다.

단점:

- 이미 적용된 migration 파일을 수정하지 않는 운영 규칙이 필요하다.
- fixture를 versioned migration으로 둘지 repeatable seed로 둘지 후속 정책이 필요하다.
- 기존 SQL 파일과 migration 파일이 일시적으로 중복된다.

### 선택지 C: Liquibase 도입

장점:

- XML/YAML/JSON changelog, rollback, diff 같은 고급 기능 선택지가 넓다.
- 스키마 변경 설명을 구조화하기 좋다.

단점:

- 현재 저장소의 SQL-first 학습 흐름보다 도구 설명 비용이 크다.
- 초기 포트폴리오 증거물 관점에서는 Flyway보다 도입 복잡도가 높다.

## 결정

`Flyway 도입`을 선택한다.

초기 구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 기본 프로필 | Flyway 비활성화 |
| PostgreSQL 프로필 | Flyway 활성화 |
| Migration 경로 | `classpath:db/migration` |
| 1차 스키마 | `V1__create_wallet_schema.sql` |
| 1차 샘플 데이터 | `V2__seed_wallet_fixture.sql` |
| 기존 SQL 파일 | H2 테스트와 수동 비교를 위해 유지 |
| 운영 규칙 | 적용된 migration은 수정하지 않고 새 migration을 추가 |

## 결과

장점:

- PostgreSQL 스키마 변경 이력이 버전 파일로 남는다.
- Testcontainers 테스트가 실제 migration 경로를 검증한다.
- 릴리스 노트에서 DB 변경 여부를 명확히 표시할 수 있다.

비용:

- 기존 `db/postgresql/schema.sql`, `fixtures.sql`와 migration 파일이 일시적으로 중복된다.
- 샘플 데이터가 versioned migration에 포함되어 운영 seed 정책과 분리 기준이 필요하다.
- 이후 스키마 변경은 migration 파일 추가 절차를 반드시 따라야 한다.

후속 작업:

- fixture를 repeatable migration 또는 별도 seed 절차로 분리할지 결정한다.
- 기존 `db/postgresql` SQL 파일의 삭제 또는 테스트 전용 전환 시점을 결정한다.
- 운영 배포에서 애플리케이션과 migration 실행 권한을 분리할지 검토한다.
