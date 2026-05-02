# Frontend E2E Test Strategy

## 목적

프론트 E2E 테스트는 사용자가 브라우저에서 실제로 보는 화면과 Spring Boot API 연결을 함께 검증한다. 단위/API 테스트가 내부 정책을 검증한다면, E2E는 화면 조작과 네트워크 연결 흐름을 검증한다.

## 실행

브라우저 설치:

```bash
cd frontend
npx playwright install chromium
```

E2E 실행:

```bash
cd frontend
npm run e2e
```

Playwright 설정은 Spring Boot 백엔드와 Vite 프론트를 자동 실행한다.

백엔드/프론트 전체 테스트 실행 순서와 실패 대응은 `local-test-guide.md`를 따른다.

## 1차 시나리오

- 초기 화면에서 `wallet-001` 잔액 `125,000 KRW`가 보인다.
- 충전 금액과 송금 금액 입력이 서로 독립적으로 동작한다.
- 충전을 실행하면 잔액이 증가한다.
- 최근 operation, step log, outbox event가 화면에 표시된다.

## 테스트 작성 기준

- selector는 가능하면 접근성 이름을 사용한다.
- 하나의 테스트는 사용자가 이해할 수 있는 업무 흐름을 표현한다.
- 모든 API edge case를 E2E에 넣지 않는다. API/도메인 edge case는 기존 Gradle 테스트에서 검증한다.
- E2E는 느리므로 릴리스 판단에 필요한 대표 흐름 중심으로 유지한다.
