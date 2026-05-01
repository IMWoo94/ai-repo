# [Feature] PostgreSQL 런타임 검증과 로컬 DB 실행 환경 추가

## 작업 요약

`postgres` 프로필의 JDBC 저장소가 실제 PostgreSQL에서도 동작하는지 검증하기 위해 Testcontainers 기반 통합 테스트와 Docker Compose 로컬 실행 환경을 추가한다.

## 배경과 문제

Issue #9에서는 PostgreSQL 스키마와 JDBC 저장소를 추가했지만, 테스트는 H2 PostgreSQL mode로 실행했다. H2는 빠르고 CI 안정성이 좋지만, 실제 PostgreSQL의 SQL 방언, 시퀀스, 트랜잭션 동작과 완전히 같지 않다.

현재 남아 있는 문제는 다음과 같다.

- 실제 PostgreSQL 컨테이너에서 스키마와 저장소가 검증되지 않았다.
- 로컬에서 `postgres` 프로필을 띄우기 위한 표준 DB 실행 방법이 없다.
- CI가 Docker 기반 통합 테스트를 실행할 준비가 되어 있는지 확인해야 한다.

## 작업 유형

인프라 기능

## 도메인 영역

저장소/검증/로컬 실행

## 범위

### 하는 것

- Testcontainers PostgreSQL 의존성을 추가한다.
- 실제 PostgreSQL 컨테이너 기반 저장소 통합 테스트를 추가한다.
- Docker Compose 로컬 PostgreSQL 실행 파일을 추가한다.
- Local Setup 문서에 Compose 기반 실행 절차를 추가한다.
- ADR과 Wiki에 H2 테스트와 PostgreSQL 컨테이너 테스트의 역할 경계를 기록한다.

### 하지 않는 것

- Flyway/Liquibase는 이번 단계에서 도입하지 않는다.
- 애플리케이션 배포 자동화는 구현하지 않는다.
- 운영 DB 백업/복구 전략은 다루지 않는다.
- 분산락과 고급 트랜잭션 격리 튜닝은 하지 않는다.

## 수용 기준

- [ ] Testcontainers 기반 PostgreSQL 저장소 테스트가 있다.
- [ ] 실제 PostgreSQL 컨테이너에서 충전 저장 흐름이 검증된다.
- [ ] 실제 PostgreSQL 컨테이너에서 송금 저장 흐름이 검증된다.
- [ ] 같은 멱등키 재시도 시 원장/감사 로그가 중복 생성되지 않음을 검증한다.
- [ ] Docker Compose로 로컬 PostgreSQL을 실행할 수 있다.
- [ ] 문서에 기본 인메모리, H2 테스트, Testcontainers, Docker Compose의 역할이 구분되어 있다.

## 도메인 규칙과 불변식

- H2 테스트는 빠른 SQL 매핑 검증용이다.
- Testcontainers 테스트는 실제 PostgreSQL 호환성 검증용이다.
- Docker Compose는 개발자가 수동으로 `postgres` 프로필을 실행하는 로컬 운영 하네스다.
- PostgreSQL 검증에서도 멱등키와 원장 중복 방지 규칙은 유지되어야 한다.

## 하네스 역할 체크

- [x] 기획자 관점에서 로컬 검증 가능한 운영 하네스 증거물을 포함했다.
- [x] 도메인 전문가 관점에서 멱등키/원장 규칙의 DB 호환성 검증을 포함했다.
- [x] 코드 개발자 A 관점에서 Testcontainers 테스트 범위를 검토했다.
- [x] 코드 개발자 B 관점에서 Compose와 CI Docker 의존성 리스크를 검토했다.
- [x] QA 관점에서 실제 PostgreSQL 기반 충전/송금/멱등성 검증을 포함했다.
- [x] 릴리스 관리자 관점에서 로컬 실행 절차와 알려진 제약을 문서화하기로 했다.

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
- [x] README 또는 Local Setup 요약 갱신이 필요하다.
- [ ] PR 설명만으로 충분하다.

## 대안과 트레이드오프

### 대안 A: H2 테스트만 유지

장점:

- 테스트가 빠르고 단순하다.
- Docker 의존성이 없다.

단점:

- PostgreSQL 실제 동작과 차이를 놓칠 수 있다.
- 런타임 검증 증거가 부족하다.

### 대안 B: Testcontainers를 추가한다

장점:

- 실제 PostgreSQL 엔진으로 저장소를 검증한다.
- CI에서 DB 호환성 회귀를 잡을 수 있다.
- 운영 하네스 증거물로 설득력이 높다.

단점:

- Docker가 필요하다.
- 테스트 시간이 늘어난다.
- 로컬 Docker 미실행 환경에서는 스킵 또는 실패 정책을 결정해야 한다.

### 현재 선호안

Testcontainers를 추가하되, Docker가 없는 로컬 환경에서는 테스트를 스킵할 수 있게 구성한다. CI에서는 Docker가 제공되므로 실제 PostgreSQL 테스트가 실행되는 것을 기대한다.

## 릴리스 고려사항

- 실행 검증: `./gradlew check`.
- 로컬 DB 검증: `docker compose up -d postgres` 후 `SPRING_PROFILES_ACTIVE=postgres ./gradlew bootRun`.
- 알려진 리스크: Docker가 없는 로컬 환경에서는 Testcontainers 테스트가 스킵될 수 있다.

## DECIDE_LATER

- Flyway/Liquibase 도입.
- PostgreSQL 기반 동시성 테스트.
- Compose에 애플리케이션 서비스까지 포함할지 여부.
- Fly.io 또는 다른 경량 배포 환경 적용.
