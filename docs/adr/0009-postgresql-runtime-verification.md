# ADR-0009: PostgreSQL Runtime Verification

## 상태

Accepted

## 맥락

ADR-0008에서 `postgres` 프로필과 JDBC 저장소를 추가했다. 해당 저장소는 H2 PostgreSQL mode 테스트로 SQL 매핑을 검증했지만, H2는 실제 PostgreSQL 엔진과 완전히 같지 않다.

멱등키, 원장, 감사 로그는 핀테크 서비스의 핵심 보존 기록이다. 따라서 실제 PostgreSQL 컨테이너에서 충전/송금/멱등성/원장 기록이 동작하는지 검증해야 한다.

## 선택지

### 선택지 A: H2 테스트만 유지

장점:

- 테스트가 빠르다.
- Docker가 필요 없다.
- CI 구성이 단순하다.

단점:

- PostgreSQL 고유 동작을 놓칠 수 있다.
- 시퀀스, 트랜잭션, SQL 방언 차이를 충분히 검증하지 못한다.
- 운영 하네스 관점의 실행 증거가 약하다.

### 선택지 B: Testcontainers 기반 PostgreSQL 테스트 추가

장점:

- 실제 PostgreSQL 엔진으로 저장소를 검증한다.
- CI에서 DB 호환성 회귀를 잡을 수 있다.
- H2 테스트보다 운영 환경에 가까운 증거를 제공한다.

단점:

- Docker가 필요하다.
- 테스트 시간이 늘어난다.
- Docker가 없는 로컬 환경에서는 스킵 정책이 필요하다.

### 선택지 C: Docker Compose 수동 검증만 사용

장점:

- 개발자가 실제 로컬 DB를 직접 확인할 수 있다.
- 애플리케이션 `postgres` 프로필 실행과 연결하기 쉽다.

단점:

- 자동화된 회귀 검증이 부족하다.
- PR마다 같은 수준의 검증을 강제하기 어렵다.

## 결정

`Testcontainers 기반 PostgreSQL 테스트`와 `Docker Compose 수동 검증 환경`을 함께 둔다.

초기 구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 자동 검증 | Testcontainers PostgreSQL 저장소 테스트 |
| 로컬 수동 검증 | `compose.yml`의 `postgres` 서비스 |
| Docker 미사용 로컬 | Testcontainers 테스트 스킵 가능 |
| CI | GitHub Actions Linux runner의 Docker 환경에서 실행 기대 |
| 마이그레이션 도구 | 이번 단계에서는 미도입 |

## 결과

장점:

- 실제 PostgreSQL에서 충전/송금/멱등성/원장 기록을 검증한다.
- 개발자가 로컬에서 `postgres` 프로필을 직접 실행할 수 있다.
- H2 테스트와 PostgreSQL 테스트의 역할이 분리된다.

비용:

- 테스트 시간이 증가한다.
- Docker 환경 문제는 개발자 로컬에서 별도 해결이 필요하다.
- Flyway/Liquibase 없이 SQL 파일을 직접 실행하는 상태는 계속 남는다.

후속 작업:

- Flyway 또는 Liquibase를 도입해 스키마 변경 이력을 관리한다.
- PostgreSQL 기반 동시성 테스트를 추가한다.
- Compose에 애플리케이션 서비스 또는 Fly.io 배포 검증을 붙일지 판단한다.
