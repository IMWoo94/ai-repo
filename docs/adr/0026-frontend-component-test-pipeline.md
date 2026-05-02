# ADR-0026: Frontend Component Test Pipeline

## 상태

Accepted

## 맥락

프론트 품질 게이트는 TypeScript/Vite build와 Playwright E2E를 갖추었다. 하지만 E2E는 실제 사용자 흐름 검증에는 강하지만 느리다. 충전/송금 금액 상태 분리, API payload 구성, 오류 메시지 표시처럼 작은 UI 회귀는 더 빠른 컴포넌트 테스트가 필요하다.

## 선택지

### 선택지 A: E2E만 유지한다

장점:

- 테스트 도구가 단순하다.
- 실제 브라우저 기준으로만 검증한다.

단점:

- 작은 상태 회귀를 잡기 위해 매번 Spring Boot와 Vite를 띄워야 한다.
- 실패 원인이 UI 상태인지 API 연결인지 구분하기 어렵다.

### 선택지 B: Vitest와 Testing Library를 추가한다

장점:

- React 컴포넌트 상태와 API 요청 payload를 빠르게 검증한다.
- 접근성 이름 기반 query를 사용해 사용자 관점의 테스트를 유지할 수 있다.
- E2E보다 실패 원인 범위가 좁다.

단점:

- jsdom 기반이라 실제 브라우저/Vite proxy 연결은 검증하지 못한다.
- fetch mock fixture 관리가 필요하다.

### 선택지 C: Storybook interaction test를 먼저 도입한다

장점:

- UI 문서화와 상호작용 테스트를 함께 얻을 수 있다.
- 디자인 QA와 연결하기 좋다.

단점:

- 현재 단계에서는 Storybook 인프라가 과하다.
- 테스트 게이트보다 UI 카탈로그 구축 범위가 커진다.

## 결정

Vitest와 Testing Library를 추가한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 테스트 러너 | Vitest |
| DOM 환경 | jsdom |
| React 테스트 | Testing Library |
| 사용자 입력 | Testing Library user-event |
| 실행 명령 | `npm run test` |
| CI 게이트 | `Frontend Unit Test` job |

## 결과

장점:

- React 상태 회귀를 E2E보다 빠르게 검증한다.
- 충전/송금 입력 독립성, 충전 payload, 잔액 부족 오류 표시가 단위 게이트로 보호된다.
- E2E는 실제 연결 흐름, 컴포넌트 테스트는 UI 상태와 payload에 집중하도록 경계가 명확해진다.

비용:

- fetch mock fixture를 유지해야 한다.
- 실제 브라우저 동작은 여전히 Playwright E2E에서 검증해야 한다.

후속 작업:

- API client가 분리되면 mock fixture를 더 작게 정리한다.
- 프론트 화면이 늘어나면 공용 render helper와 fixture builder를 추가한다.
