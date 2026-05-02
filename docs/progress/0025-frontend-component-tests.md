# 0025. Frontend Component Tests

## 스펙 목표

- React 사용자 화면의 상태 회귀를 빠르게 검증하는 컴포넌트 테스트를 추가한다.
- 충전/송금 금액 입력 독립성, 충전 payload, 잔액 부족 오류 표시를 단위 게이트로 보호한다.
- CI에 프론트 단위 테스트 job을 추가한다.

## 완료 결과

- Vitest, Testing Library, user-event, jest-dom, jsdom을 추가했다.
- `npm run test`와 `npm run test:watch` 스크립트를 추가했다.
- `App` 컴포넌트 테스트를 추가했다.
- fetch mock으로 초기 조회, 충전 성공, 송금 실패 응답을 검증한다.
- GitHub Actions에 `Frontend Unit Test` job을 추가했다.
- ADR-0026과 로컬 테스트 가이드를 갱신했다.

## 검증

- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `./gradlew test scenarioTest check`

## 남은 일

- 화면이 늘어나면 공용 frontend test fixture builder를 분리한다.
- API client가 분리되면 fetch mock을 client mock으로 축소한다.

## 관련 문서

- `docs/adr/0026-frontend-component-test-pipeline.md`
- `docs/testing/local-test-guide.md`
- `issue-drafts/0025-frontend-component-tests.md`
