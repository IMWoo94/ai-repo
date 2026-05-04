# React User Frontend

## 목적

이 화면은 백엔드 API가 만든 금융 도메인 결과를 사람이 직접 검증하기 위한 사용자 화면이다. 잔액, 거래내역, 충전, 송금, 원장, 감사 로그, step log, outbox event를 하나의 흐름으로 확인한다.

운영자 콘솔 영역에서는 manual review outbox를 조회하고, requeue를 실행하며, requeue audit trail을 확인한다.

## 디자인 기준

- UI 생성과 수정은 `DESIGN-apple.md`를 따른다.
- 화면은 Apple 스타일의 큰 타이포그래피, 절제된 색상, pill 버튼, light/dark tile 대비를 사용한다.
- 문서와 코드 주석에는 로컬 내부 절대경로를 남기지 않는다.

## 로컬 실행

백엔드 실행:

```bash
./gradlew bootRun
```

프론트 실행:

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`을 연다. Vite proxy가 `/api` 요청을 Spring Boot의 `http://localhost:8080`으로 전달한다.

## 검증

전체 백엔드/프론트 테스트 실행 순서는 `local-test-guide.md`를 우선 따른다.

프론트 빌드:

```bash
cd frontend
npm run test
npm run build
```

브라우저 E2E:

```bash
cd frontend
npx playwright install chromium
npm run e2e
```

전체 품질 게이트:

```bash
./gradlew test
./gradlew scenarioTest
./gradlew check
cd frontend && npm run test
cd frontend && npm run build
cd frontend && npm run e2e
```

## 현재 범위

- 지갑 ID 기준 잔액 조회
- 거래내역 조회
- 충전 요청
- 송금 요청
- 원장, 감사 로그, step log, outbox event 조회
- 운영자 token/operator header 입력
- manual review outbox 조회
- manual review outbox requeue
- requeue audit trail 조회

## 범위 제외

- 실제 로그인/OIDC/IAM
- 운영자 승인 워크플로우
- React 라우팅
- 운영 배포 파이프라인

## 운영자 콘솔

운영자 콘솔은 현재 백엔드의 header 기반 운영 API를 그대로 사용한다.

| 입력 | 기본값 | 용도 |
| --- | --- | --- |
| Admin token | `local-ops-token` | `X-Admin-Token` header |
| Operator ID | `local-operator` | `X-Operator-Id` header |
| Requeue reason | `broker recovered from operator console` | requeue audit reason |

화면 상태는 다음 기준으로 표시한다.

- manual review event가 없으면 empty state를 표시한다.
- API 오류는 error callout으로 표시한다.
- outbox status는 status badge로 표시한다.
- requeue 성공 후에는 선택한 event의 audit trail을 유지해서 운영 조치 증거를 확인한다.
