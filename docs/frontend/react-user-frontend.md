# React User Frontend

## 목적

이 화면은 백엔드 API가 만든 금융 도메인 결과를 사람이 직접 검증하기 위한 사용자 화면이다. 잔액, 거래내역, 충전, 송금, 원장, 감사 로그, step log, outbox event를 하나의 흐름으로 확인한다.

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

프론트 빌드:

```bash
cd frontend
npm run build
```

전체 품질 게이트:

```bash
./gradlew test
./gradlew scenarioTest
./gradlew check
cd frontend && npm run build
```

## 현재 범위

- 지갑 ID 기준 잔액 조회
- 거래내역 조회
- 충전 요청
- 송금 요청
- 원장, 감사 로그, step log, outbox event 조회

## 범위 제외

- 인증/인가
- 운영자 승인 화면
- React 라우팅
- 프론트 단위 테스트
- 운영 배포 파이프라인
