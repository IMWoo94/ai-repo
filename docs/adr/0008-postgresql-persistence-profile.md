# ADR-0008: PostgreSQL Persistence Profile

## 상태

Accepted

## 맥락

현재 애플리케이션은 인메모리 저장소로 충전, 송금, 멱등키, 원장, 감사 로그를 처리한다. 이 구조는 빠른 학습과 테스트에는 유리하지만 프로세스 재시작 시 핵심 금융 기록이 모두 사라진다.

멱등키와 원장은 프로세스 수명과 분리되어 보존되어야 한다. 다만 PostgreSQL을 기본 저장소로 즉시 전환하면 로컬 개발과 CI가 DB 실행에 강하게 의존하게 된다.

## 선택지

### 선택지 A: PostgreSQL을 기본 저장소로 즉시 전환

장점:

- 실제 운영 환경과 가까운 구조가 된다.
- 저장소 구현이 하나로 줄어든다.
- 멱등키와 원장 기록이 기본적으로 영속화된다.

단점:

- 모든 로컬 실행과 CI가 PostgreSQL에 의존한다.
- Docker Compose, Testcontainers, 마이그레이션 도구 결정이 선행되어야 한다.
- 아직 학습 단계에서 개발 피드백 속도가 떨어진다.

### 선택지 B: 기본은 인메모리, `postgres` 프로필에서 JDBC 저장소 사용

장점:

- 기존 기본 실행과 CI 안정성을 유지한다.
- PostgreSQL 스키마와 저장소 매핑을 점진적으로 검증할 수 있다.
- DB 전환 리스크를 코드, 테스트, 문서로 분리할 수 있다.

단점:

- 인메모리 저장소와 JDBC 저장소를 함께 유지해야 한다.
- H2 테스트와 실제 PostgreSQL의 차이가 남는다.
- 실제 PostgreSQL smoke 검증은 별도 실행 환경이 필요하다.

### 선택지 C: 마이그레이션 도구와 Testcontainers를 먼저 도입

장점:

- 실제 PostgreSQL 기반 검증 품질이 높다.
- 스키마 버전 관리와 CI 검증을 함께 가져갈 수 있다.

단점:

- 이번 작업 범위가 커진다.
- Docker 환경 의존성이 생긴다.
- 원장/멱등키 저장소의 첫 매핑 검증보다 도구 도입 논의가 앞설 수 있다.

## 결정

`기본은 인메모리, postgres 프로필에서 JDBC 저장소 사용`을 선택한다.

초기 구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 기본 프로필 | 인메모리 저장소 |
| PostgreSQL 프로필 | `postgres` |
| 저장소 구현 | Spring JDBC 기반 `JdbcWalletRepository` |
| 스키마 파일 | `src/main/resources/db/postgresql/schema.sql` |
| 샘플 데이터 | `src/main/resources/db/postgresql/fixtures.sql` |
| 테스트 방식 | H2 PostgreSQL mode 기반 저장소 테스트 |
| 마이그레이션 도구 | 이번 단계에서는 미도입 |

## 결과

장점:

- 기본 로컬 실행과 CI가 계속 빠르게 동작한다.
- PostgreSQL 전환에 필요한 테이블 경계가 코드로 검증된다.
- 멱등키, 원장, 감사 로그 영속화의 첫 기준 구현이 생긴다.

비용:

- H2와 PostgreSQL 방언 차이는 남는다.
- 실제 PostgreSQL 컨테이너 기반 검증은 후속 작업으로 남는다.
- 스키마 변경 이력 관리는 아직 SQL 파일 수동 관리에 가깝다.

후속 작업:

- Flyway 또는 Liquibase 도입 여부를 결정한다.
- Testcontainers로 실제 PostgreSQL 저장소 테스트를 추가한다.
- 원장 append-only 제약과 멱등키 유니크 제약을 운영 DB 기준으로 강화한다.
