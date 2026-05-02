# 0030. Admin API Access Audit

## 스펙 목표

- 운영 API 접근 성공/실패를 구조화된 감사 로그로 남긴다.
- 운영자 식별자는 `X-Operator-Id`에서 기록하되 admin token은 저장하지 않는다.
- 최근 운영 API 접근 감사 로그를 운영 API로 조회한다.

## 완료 결과

- `AdminApiAccessAudit`과 `AdminApiAccessOutcome`을 추가했다.
- `AdminApiAccessAuditService`를 추가해 method, path, operatorId, statusCode, outcome을 기록한다.
- `AdminApiAccessAuditFilter`를 추가해 outbox 운영 API와 접근 감사 조회 API 호출을 기록한다.
- 인메모리 저장소와 JDBC 저장소에 접근 감사 저장/조회 기능을 추가했다.
- PostgreSQL Flyway `V10__create_admin_api_access_audits.sql`을 추가했다.
- `GET /api/v1/admin-api-access-audits` 운영 API를 추가했다.
- 단위/API/JDBC 테스트로 성공 접근, 실패 접근, 권한, 저장소 조회를 검증했다.

## 검증

- `./gradlew test --tests '*AdminApiAccessAuditServiceTest' --tests '*AdminApiAccessAuditControllerTest' --tests '*JdbcWalletRepositoryTest'`
- `./gradlew test scenarioTest check`
- `git diff --check`

## 남은 일

- 감사 로그 보존 기간과 pruning 정책을 추가한다.
- IP 주소와 User-Agent 저장 여부를 별도 ADR로 결정한다.
- Spring Security 전환 시 principal/role 정보를 감사 로그와 연결한다.
- 운영자 화면에서 접근 감사 이력을 조회할 수 있게 한다.

## 관련 문서

- `docs/adr/0031-admin-api-access-audit.md`
- `docs/development/local-setup.md`
- `issue-drafts/0030-admin-api-access-audit.md`
