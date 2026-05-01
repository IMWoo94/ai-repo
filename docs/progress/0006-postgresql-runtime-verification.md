# 0006. PostgreSQL 런타임 검증

## 스펙 목표

- PostgreSQL 프로필이 실제 컨테이너 환경에서도 동작하는지 검증한다.
- 로컬 개발자가 IntelliJ 또는 CLI에서 같은 방식으로 실행할 수 있어야 한다.
- Docker Compose와 Testcontainers를 통해 운영 유사성을 높인다.

## 완료 결과

- `compose.yml`에 PostgreSQL 서비스를 추가했다.
- Testcontainers 기반 PostgreSQL repository 테스트를 추가했다.
- 로컬 실행 문서에 PostgreSQL 프로필 실행 방식을 정리했다.

## 검증

- GitHub Actions에서 Gradle check가 통과했다.
- 로컬 Docker daemon이 켜져 있으면 Testcontainers 검증이 가능하다.

## 남은 일

- 로컬 Docker daemon이 꺼져 있으면 컨테이너 검증은 실행되지 않는다.
- 릴리스 전 Docker 실행 검증을 명시적인 게이트로 고정해야 한다.

## 관련 문서

- `docs/adr/0009-postgresql-runtime-verification.md`
- `docs/development/local-setup.md`
- `issue-drafts/0006-postgresql-runtime-verification.md`
