# [Feature] React 사용자 화면 프론트 MVP 추가

GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/43

## 배경

현재 백엔드는 잔액/거래내역, 충전/송금, 원장, 감사 로그, step log, outbox event API를 제공한다. 포트폴리오형 학습 프로젝트로 발전시키려면 API 결과를 사용자가 직접 확인할 수 있는 화면이 필요하다.

## 목표

- React 기반 사용자 화면을 추가한다.
- `DESIGN-apple.md` 기준의 Apple 스타일 UI를 적용한다.
- 지갑 조회, 충전, 송금, 원장/감사/운영 증거 조회 흐름을 화면에서 연결한다.
- 프론트 실행/검증 방법을 문서화한다.
- CI에서 프론트 build를 검증한다.

## 범위

- `frontend` React/Vite 앱
- Vite proxy 기반 로컬 백엔드 연동
- 사용자 지갑 조회/충전/송금 화면
- 거래내역/원장/감사 로그/step log/outbox event 확인 영역
- ADR/Progress/README 문서 갱신
- GitHub Actions frontend build job

## 범위 제외

- 인증/인가
- 운영자 manual review 화면
- React 라우팅
- 프론트 단위 테스트
- 운영 배포 파이프라인

## 수용 기준

- [ ] `AGENTS.md`에 UI 생성 기준이 파일명으로만 명시되어 있다.
- [ ] `frontend`에서 React/Vite 앱이 빌드된다.
- [ ] 화면에서 잔액 조회, 충전, 송금, 거래/원장/감사/운영 증거 조회 흐름을 사용할 수 있다.
- [ ] `docs/adr/0024-react-user-frontend-mvp.md`에 선택 근거와 트레이드오프가 기록되어 있다.
- [ ] `docs/frontend/react-user-frontend.md`에 로컬 실행과 검증 방법이 기록되어 있다.
- [ ] `./gradlew test scenarioTest check`와 `cd frontend && npm run build`가 통과한다.
