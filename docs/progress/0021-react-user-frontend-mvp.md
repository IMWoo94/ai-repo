# 0021. React User Frontend MVP

## 스펙 목표

- React 기반 사용자 화면을 추가해 백엔드 API 결과를 직접 검증한다.
- UI 생성 기준을 `AGENTS.md`에 명시하고 `DESIGN-apple.md`를 기준으로 삼는다.
- 프론트 실행/검증 방법을 문서화하고 CI build 게이트를 추가한다.

## 완료 결과

- `frontend`에 React, TypeScript, Vite 기반 사용자 화면을 추가했다.
- Vite proxy로 `/api` 요청을 Spring Boot 로컬 서버에 전달하도록 구성했다.
- 화면에서 잔액, 거래내역, 충전, 송금, 원장, 감사 로그, step log, outbox event를 조회할 수 있게 했다.
- `AGENTS.md`에 UI 생성 규칙과 내부 절대경로 금지 규칙을 추가했다.
- ADR-0024에 React/Vite 선택 근거와 대안을 기록했다.
- GitHub Actions에 `Frontend Build` job을 추가했다.

## 검증

- `cd frontend && npm run build`로 TypeScript와 Vite build를 검증한다.
- `./gradlew test scenarioTest check`로 기존 백엔드 회귀를 검증한다.
- CI에서 Gradle Check, Scenario Test, Frontend Build를 분리 실행한다.

## 남은 일

- 프론트 단위/컴포넌트 테스트 도입은 별도 작업에서 결정한다.
- 운영자용 outbox manual review 화면은 별도 UI 단계에서 추가한다.
- 실제 배포 방식은 정적 호스팅, Spring resource packaging, 별도 컨테이너 중 하나로 후속 결정한다.

## 관련 문서

- `docs/adr/0024-react-user-frontend-mvp.md`
- `docs/frontend/react-user-frontend.md`
- `issue-drafts/0021-react-user-frontend-mvp.md`
