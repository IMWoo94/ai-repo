# 0022. Frontend E2E Test Pipeline

## 스펙 목표

- React 사용자 화면 프로토타입을 브라우저 기준으로 검증한다.
- Spring Boot API와 Vite proxy 연결을 E2E로 확인한다.
- 충전/송금 금액 입력 독립성 회귀를 자동화한다.

## 완료 결과

- Playwright 기반 E2E 테스트를 추가했다.
- `npm run e2e` 명령을 추가했다.
- Playwright `webServer`로 Spring Boot와 Vite dev server를 자동 실행하도록 구성했다.
- 충전 금액/송금 금액 필드에 접근성 이름을 부여했다.
- GitHub Actions에 `Frontend E2E` job을 추가했다.
- ADR-0025와 E2E 테스트 전략 문서를 추가했다.

## 검증

- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `./gradlew test scenarioTest check`

## 남은 일

- manual review fixture 생성 방식이 정리되면 운영자 requeue 성공 E2E를 추가한다.

## 관련 문서

- `docs/adr/0025-frontend-e2e-test-pipeline.md`
- `docs/testing/frontend-e2e-test-strategy.md`
- `issue-drafts/0022-frontend-e2e-test-pipeline.md`
