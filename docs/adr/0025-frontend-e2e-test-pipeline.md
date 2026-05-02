# ADR-0025: Frontend E2E Test Pipeline

## 상태

Accepted

## 맥락

React 사용자 화면 MVP가 추가되면서 단순 build smoke만으로는 실제 사용자 흐름을 검증하기 어렵다. 특히 브라우저에서 Vite proxy가 Spring Boot API로 요청을 전달하고, 충전 이후 잔액과 operation 증거가 화면에 반영되는지 확인해야 한다.

## 선택지

### 선택지 A: 수동 브라우저 확인만 유지한다

장점:

- 도구 추가가 없다.
- 빠르게 화면을 눈으로 확인할 수 있다.

단점:

- 충전/송금 금액 상태 공유 같은 회귀를 자동으로 잡지 못한다.
- CI에서 사용자 흐름 품질을 보장하지 못한다.

### 선택지 B: Playwright로 브라우저 E2E를 추가한다

장점:

- 실제 Chromium 브라우저에서 사용자 조작을 검증한다.
- Spring Boot와 Vite dev server를 자동 실행해 로컬/CI 실행 방식이 일치한다.
- 접근성 이름 기반 selector를 사용해 UI 의미도 같이 검증한다.

단점:

- 브라우저 설치와 서버 기동 시간이 CI 비용을 늘린다.
- 테스트가 API/프론트 양쪽에 걸쳐 실패 원인 분석 범위가 넓어진다.

### 선택지 C: Mock API 기반 컴포넌트 테스트만 추가한다

장점:

- 빠르고 안정적이다.
- UI 상태 로직을 좁은 범위로 검증할 수 있다.

단점:

- Vite proxy와 Spring Boot API 연결을 검증하지 못한다.
- 프로토타입을 실제로 띄웠을 때의 흐름 보장이 약하다.

## 결정

Playwright 기반 브라우저 E2E를 추가한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 도구 | Playwright |
| 브라우저 | Chromium |
| 실행 명령 | `npm run e2e` |
| 서버 기동 | Playwright `webServer`에서 Spring Boot와 Vite 자동 실행 |
| CI 게이트 | `Frontend E2E` job |
| 1차 시나리오 | 초기 잔액 조회, 금액 입력 독립성, 충전, 원장/step/outbox 증거 확인 |

## 결과

장점:

- 프로토타입 화면이 실제 백엔드 API와 연결되는지 자동 검증한다.
- 충전/송금 금액 입력 독립성 회귀를 브라우저 수준에서 보호한다.
- 사용자가 검증할 핵심 흐름을 CI 품질 게이트로 승격한다.

비용:

- CI에 Playwright 브라우저 설치 시간이 추가된다.
- E2E 테스트는 단위 테스트보다 느리므로 핵심 사용자 흐름 중심으로 유지해야 한다.

후속 작업:

- 송금 성공 흐름과 실패 흐름을 별도 E2E로 추가한다.
- 운영자 manual review 화면이 생기면 outbox requeue E2E를 추가한다.
- flake가 발생하면 trace artifact 보관 정책을 강화한다.
