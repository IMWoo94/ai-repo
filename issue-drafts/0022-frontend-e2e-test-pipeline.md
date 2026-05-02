# [Feature] React 사용자 화면 E2E 테스트 추가

GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/47

## 배경

React 사용자 화면 MVP가 추가되었으므로 브라우저 기준의 실제 사용자 흐름 검증이 필요하다. 현재 CI는 프론트 build smoke와 백엔드 API/시나리오 테스트를 분리 검증하지만, 브라우저에서 Vite proxy를 통해 Spring Boot API까지 연결되는지는 검증하지 않는다.

## 목표

- Playwright 기반 E2E 테스트를 추가한다.
- 로컬에서 Spring Boot 백엔드와 Vite 프론트를 자동 실행해 브라우저 흐름을 검증한다.
- CI에 Frontend E2E job을 추가한다.
- 충전/송금 금액 필드가 독립적으로 동작하는지 회귀 검증한다.

## 범위

- `frontend` Playwright 설정
- 사용자 화면 E2E 시나리오
- npm script 추가
- CI E2E job 추가
- ADR/Progress/Testing 문서 갱신

## 범위 제외

- 모든 API edge case의 E2E화
- 시각 회귀 테스트
- 운영자 manual review 화면 테스트
- 배포 환경 E2E

## 수용 기준

- [ ] `npm --prefix frontend run e2e`로 E2E 테스트가 실행된다.
- [ ] E2E가 백엔드와 프론트 dev server를 자동 실행한다.
- [ ] 초기 잔액 조회가 화면에 표시된다.
- [ ] 충전 금액과 송금 금액 입력이 서로 독립적으로 동작한다.
- [ ] 충전 후 잔액과 operation 증거가 갱신된다.
- [ ] CI에서 Frontend E2E job이 통과한다.
