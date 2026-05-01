# 0007. Flyway 스키마 마이그레이션

## 스펙 목표

- PostgreSQL 스키마 변경을 수동 SQL이 아니라 Flyway migration으로 관리한다.
- 이후 기능 추가 시 DB 변경 이력이 코드와 함께 추적되어야 한다.
- H2 테스트 스키마는 빠른 테스트를 위해 유지한다.

## 완료 결과

- Flyway 의존성과 migration 설정을 추가했다.
- 초기 schema migration을 `src/main/resources/db/migration` 아래로 옮겼다.
- 기존 PostgreSQL SQL은 수동 비교와 테스트 보조 용도로 정리했다.

## 검증

- Gradle check로 migration 포함 빌드 경로를 검증했다.
- JDBC repository 테스트가 migration 기준 스키마와 호환되는지 확인했다.

## 남은 일

- migration rollback 전략은 아직 정의하지 않았다.
- 운영 배포 전 migration dry-run 절차가 필요하다.

## 관련 문서

- `docs/adr/0010-flyway-schema-migrations.md`
- `issue-drafts/0007-flyway-schema-migrations.md`
- `src/main/resources/db/migration`
