# 0027. Admin API Authz

## 스펙 목표

- Outbox manual review와 requeue API를 운영자 전용 API로 구분한다.
- 인증 실패와 권한 실패를 `401`, `403`으로 분리한다.
- requeue audit의 operator를 요청 body가 아니라 인증된 운영자 header에서 기록한다.

## 완료 결과

- `X-Admin-Token`, `X-Operator-Id` 기반 운영 API guard를 추가했다.
- 운영 API controller가 manual review 조회, requeue, requeue audit 조회 전에 guard를 호출한다.
- `AI_REPO_OPS_ADMIN_TOKEN` 환경 변수로 로컬 기본 token을 override할 수 있게 했다.
- 인증 실패를 `ADMIN_AUTHENTICATION_REQUIRED`, 권한 실패를 `ADMIN_AUTHORIZATION_DENIED`로 응답한다.
- admin token은 상수시간 비교로 검증한다.
- requeue 요청 body에서 operator를 제거하고, audit operator는 `X-Operator-Id`에서 가져오도록 변경했다.
- 운영 API 성공, token 누락, token 오류, operator 누락, requeue audit operator 기록 테스트를 추가했다.

## 검증

- `./gradlew test --tests '*OperationOutboxReviewControllerTest' --tests '*WalletApiExceptionHandlerTest' --tests '*WalletScenarioFlowTest'`

## 남은 일

- Spring Security 기반 role 모델을 검토한다.
- 운영자 requeue 승인 workflow를 추가한다.
- 운영 API 접근 로그를 별도 audit event로 남긴다.

## 관련 문서

- `docs/adr/0028-admin-api-authz.md`
- `docs/testing/local-test-guide.md`
- `issue-drafts/0027-admin-api-authz.md`
