# 0033. Spring Security Role Model

## 스펙 목표

- 운영 API 인증/인가를 Spring Security filter chain으로 이동한다.
- 기존 `X-Admin-Token`, `X-Operator-Id` contract와 오류 응답 code는 유지한다.
- 운영 API 위험도에 따라 `ROLE_OPERATOR`, `ROLE_ADMIN` 경계를 만든다.

## 완료 결과

- `spring-boot-starter-security`와 `spring-security-test`를 추가했다.
- `SecurityConfig`를 추가해 stateless security filter chain을 구성했다.
- `AdminHeaderAuthenticationFilter`를 추가해 admin header를 Spring Security authentication으로 변환한다.
- `AdminSecurityRole`로 `OPERATOR`, `ADMIN` 역할을 명시했다.
- `AdminSecurityErrorHandler`로 401/403 JSON 오류 응답을 유지했다.
- 운영 controller의 수동 guard 호출을 제거하고 보안 판단을 filter chain으로 이동했다.
- `AdminApiAccessAuditFilter`를 highest precedence로 조정해 security 401/403도 접근 감사에 남긴다.
- 단위/API 테스트로 role 부여, 401/403, 일반 API 공개 접근, admin access audit 기록을 검증했다.

## 검증

- `./gradlew test --tests '*AdminHeaderAuthenticationFilterTest' --tests '*OperationOutboxReviewControllerTest' --tests '*OperationOutboxRelayRunControllerTest' --tests '*AdminApiAccessAuditControllerTest' --tests '*OperationalLogPruningControllerTest' --tests '*WalletQueryControllerTest' --tests '*WalletCommandControllerTest'`
- `./gradlew test scenarioTest check`
- `git diff --check`

## 남은 일

- operator/admin token 분리 여부를 결정한다.
- JWT/OIDC 전환 시 principal/role/audit operator 연결을 설계한다.
- pruning API를 승인 워크플로우와 연결한다.

## 관련 문서

- `docs/adr/0034-spring-security-role-model.md`
- `docs/development/local-setup.md`
- `issue-drafts/0033-spring-security-role-model.md`
