# [Feature] 프론트 컴포넌트 테스트 추가

GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/53

## 배경

현재 프론트 품질 게이트는 build smoke와 Playwright E2E 중심이다. E2E는 실제 흐름 검증에는 좋지만 느리기 때문에, React 상태와 API 요청 payload 같은 작은 회귀를 빠르게 잡는 컴포넌트 테스트가 필요하다.

## 목표

- Vitest와 Testing Library 기반 프론트 컴포넌트 테스트를 추가한다.
- 초기 지갑 조회 렌더링을 검증한다.
- 충전/송금 금액 입력 독립성을 빠른 테스트로 보호한다.
- 충전 요청 payload와 operation 증거 갱신을 검증한다.
- 잔액 부족 송금 오류 메시지 표시를 검증한다.
- CI에 프론트 단위 테스트 게이트를 추가한다.

## 범위

- `frontend/src/App.test.tsx`
- Vitest 설정
- npm test scripts
- GitHub Actions `Frontend Unit Test` job
- ADR/Progress/Testing 문서 갱신

## 범위 제외

- 신규 UI 구현
- Storybook 도입
- visual regression test
- API client 분리

## 수용 기준

- [ ] `npm --prefix frontend run test`가 통과한다.
- [ ] `npm --prefix frontend run build`가 통과한다.
- [ ] 초기 잔액 렌더링 테스트가 있다.
- [ ] 충전/송금 금액 입력 독립성 테스트가 있다.
- [ ] 충전 API payload 테스트가 있다.
- [ ] 잔액 부족 송금 오류 표시 테스트가 있다.
- [ ] CI에서 프론트 단위 테스트가 실행된다.
