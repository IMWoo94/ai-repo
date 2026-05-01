# [Feature] Flyway 기반 스키마 버전 관리 도입

## 작업 요약

PostgreSQL 스키마와 샘플 데이터를 Spring SQL init에서 Flyway migration으로 이전한다. `postgres` 프로필은 Flyway를 통해 DB 스키마를 관리하고, 기존 수동 SQL 파일은 테스트 호환성을 위해 유지하되 migration 파일을 기준 문서로 삼는다.

## 배경과 문제

Issue #9와 #11에서 PostgreSQL 저장소, 스키마 SQL, Testcontainers 검증을 추가했다. 하지만 현재 `postgres` 프로필은 `spring.sql.init`로 `schema.sql`과 `fixtures.sql`을 실행한다. 이 방식은 초기 스키마에는 충분하지만, 변경 이력과 순서를 추적하기 어렵다.

현재 남아 있는 문제는 다음과 같다.

- 스키마 변경 이력이 버전으로 관리되지 않는다.
- 운영 DB에 이미 적용된 스키마와 새 스키마의 차이를 추적하기 어렵다.
- PostgreSQL 스키마 변경이 늘어날수록 수동 SQL init 방식의 위험이 커진다.

## 작업 유형

인프라 기능

## 도메인 영역

저장소/스키마 관리

## 범위

### 하는 것

- Flyway 의존성을 추가한다.
- `postgres` 프로필에서 Flyway를 활성화한다.
- 기존 schema SQL을 `V1__create_wallet_schema.sql` migration으로 복제한다.
- 기존 fixture SQL을 `V2__seed_wallet_fixture.sql` migration으로 복제한다.
- Testcontainers PostgreSQL 테스트가 Flyway migration으로 DB를 준비하도록 변경한다.
- Local Setup, ADR, Wiki에 Flyway 운영 기준을 기록한다.

### 하지 않는 것

- Liquibase는 도입하지 않는다.
- 기존 H2 빠른 저장소 테스트를 제거하지 않는다.
- 기존 `db/postgresql/schema.sql`, `fixtures.sql` 파일은 즉시 삭제하지 않는다.
- 운영 배포 자동화는 구현하지 않는다.

## 수용 기준

- [ ] Flyway 의존성이 추가되어 있다.
- [ ] `postgres` 프로필에서 `spring.sql.init` 대신 Flyway migration을 사용한다.
- [ ] `V1__create_wallet_schema.sql`이 1차 스키마를 생성한다.
- [ ] `V2__seed_wallet_fixture.sql`이 샘플 데이터를 넣는다.
- [ ] Testcontainers PostgreSQL 테스트가 Flyway로 스키마와 fixture를 적용한다.
- [ ] `./gradlew check`가 통과한다.
- [ ] 문서에 Flyway와 기존 SQL 파일의 역할 경계가 명시되어 있다.

## 도메인 규칙과 불변식

- 스키마 변경은 버전이 있는 migration으로 남긴다.
- 이미 적용된 migration은 수정하지 않고 새 migration으로 변경한다.
- 샘플 데이터는 개발/검증 목적이며 운영 데이터 정책을 대체하지 않는다.

## 하네스 역할 체크

- [x] 기획자 관점에서 운영 하네스 증거물로 스키마 이력 관리를 선택했다.
- [x] 도메인 전문가 관점에서 원장/멱등키 테이블 변경 이력의 중요성을 검토했다.
- [x] 코드 개발자 A 관점에서 기존 SQL init과 Flyway 전환 범위를 검토했다.
- [x] 코드 개발자 B 관점에서 H2 테스트 호환성과 PostgreSQL migration 경계를 검토했다.
- [x] QA 관점에서 Testcontainers가 Flyway 경로를 검증하도록 범위를 잡았다.
- [x] 릴리스 관리자 관점에서 migration 수정 금지 원칙을 문서화하기로 했다.

## 예상 테스트 범위

- [ ] 단위 테스트가 필요하다.
- [x] Repository 통합 테스트가 필요하다.
- [ ] 동시성 테스트가 필요하다.
- [x] 멱등성 테스트가 필요하다.
- [ ] 회귀 테스트가 필요하다.
- [x] 릴리스 실행 검증이 필요하다.
- [ ] 코드 변경이 없는 문서 작업이다.

## 문서화 필요 여부

- [x] ADR이 필요하다.
- [x] Wiki 사고 과정 기록이 필요하다.
- [x] Local Setup 갱신이 필요하다.
- [ ] PR 설명만으로 충분하다.

## 대안과 트레이드오프

### 대안 A: Spring SQL init 유지

장점:

- 구현이 가장 단순하다.
- 현재 테스트와 설정 변경이 적다.

단점:

- 변경 이력 추적이 어렵다.
- 운영 DB의 스키마 상태를 안전하게 관리하기 어렵다.

### 대안 B: Flyway 도입

장점:

- SQL-first 방식이라 현재 스키마 파일과 자연스럽게 이어진다.
- migration 순서와 적용 이력을 관리할 수 있다.
- Spring Boot와 통합이 단순하다.

단점:

- migration 파일 수정 금지 원칙을 지켜야 한다.
- fixture를 migration으로 둘지 별도 seed로 둘지 정책이 필요하다.

### 대안 C: Liquibase 도입

장점:

- XML/YAML/JSON 등 구조화된 changelog를 사용할 수 있다.
- rollback, diff 등 고급 기능 선택지가 넓다.

단점:

- 현재 SQL-first 학습 흐름보다 도구 복잡도가 크다.
- 초기 포트폴리오 증거물에는 Flyway보다 설명 비용이 높다.

### 현재 선호안

Flyway를 선택한다. 현재 저장소는 직접 SQL로 스키마를 작성하고 있으므로 SQL-first migration이 가장 단순하고 검증 가능하다.

## 릴리스 고려사항

- 실행 검증: `./gradlew check`.
- PostgreSQL 실행 검증: Testcontainers 기반 PostgreSQL 테스트.
- 알려진 리스크: 기존 `schema.sql`, `fixtures.sql`와 migration 파일의 중복이 일시적으로 존재한다.

## DECIDE_LATER

- fixture를 Flyway repeatable migration으로 분리할지 여부.
- 기존 `db/postgresql/schema.sql`, `fixtures.sql` 삭제 시점.
- 운영 배포에서 migration 실행 권한을 분리할지 여부.
