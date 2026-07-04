# Getting Started

> ai-repo를 처음 실행하는 단일 진입점입니다. 구조는 [Architecture Overview](ARCHITECTURE.md), 용어는 [Glossary](GLOSSARY.md), IDE 설정은 [Local Setup](development/local-setup.md)을 참고하세요.

## 사전 요구

- **JDK 25** (Gradle toolchain이 자동 프로비저닝하지만, 로컬 JDK 25가 있으면 빠름)
- **Node.js 20+** (프런트엔드)
- **Docker Desktop** (PostgreSQL 프로필 / 시나리오 테스트 / 예정된 k8s 모니터링)

## 1. 백엔드 실행 (기본: 인메모리)

```bash
# 저장소 루트에서
./gradlew bootRun
# 헬스 체크
curl -fsS http://localhost:8080/actuator/health
```

기본 실행은 **인메모리 저장소**를 사용합니다. 재시작하면 데이터가 초기화됩니다.

## 2. 프런트엔드 실행

```bash
cd frontend
npm install
npm run dev        # Vite dev server (백엔드로 프록시)
```

브라우저에서 지갑 화면 + 운영자 콘솔을 확인합니다.

## 3. 엔드유저 흐름 (로그인 → 충전 → 송금)

```bash
# 로그인 → JWT 발급 (memberId 기준)
TOKEN=$(curl -fsS -X POST http://localhost:8080/api/v1/auth/tokens \
  -H 'Content-Type: application/json' \
  -d '{"memberId":"member-001"}' | jq -r .accessToken)

# 잔액 조회 (Bearer)
curl -fsS http://localhost:8080/api/v1/wallets/wallet-001/balance \
  -H "Authorization: Bearer $TOKEN"

# 충전 (멱등키 필수)
curl -fsS -X POST http://localhost:8080/api/v1/wallets/wallet-001/charges \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: charge-demo-001' \
  -d '{"amount":"10000","currency":"KRW"}'
```

> 실제 요청 필드/픽스처 준비는 코드 기준이 정확합니다 — `AuthTokenController`, `WalletCommandController`, `TestFixtureController`(`POST /api/v1/test-fixtures`, property 게이트)를 참고하세요. 전체 엔드포인트 목록은 [README](../README.md#로컬-실행)에 있습니다.

## 4. PostgreSQL 프로필 (행 잠금·Flyway 검증)

```bash
docker compose -f compose.yml up -d postgres          # postgres:17
SPRING_PROFILES_ACTIVE=postgres ./gradlew bootRun     # Flyway V1..V18 적용
```

인메모리 대신 실제 잠금/동시성 경로(`SELECT..FOR UPDATE`, `lock_timeout`)를 검증할 때 사용합니다.

## 5. 품질 게이트

```bash
./gradlew test                 # 단위/API/저장소 빠른 회귀
./gradlew scenarioTest         # 대표 사용자/운영 흐름 시나리오
./gradlew postgresScenarioTest # Docker/Testcontainers 실제 PostgreSQL 흐름
./gradlew check                # 전체 검증

cd frontend
npm run test                   # vitest (컴포넌트/상태)
npm run build                  # TypeScript/Vite smoke
npm run e2e                    # Playwright E2E
```

상세 순서·실패 대응은 [Local Test Guide](testing/local-test-guide.md), 시나리오 추가 기준은 [Scenario Test Strategy](testing/scenario-test-strategy.md)를 따릅니다.

## 6. 운영 API (선택)

Outbox 릴레이/컨슈머/알림 스케줄러는 **기본 비활성**입니다. 로컬에서 자동 발행을 보려면:

```bash
AI_REPO_OUTBOX_RELAY_SCHEDULER_ENABLED=true ./gradlew bootRun
```

운영 API는 `X-Operator-Token`/`X-Admin-Token`/`X-Operator-Id` 헤더를 사용합니다(로컬 기본 토큰 `local-operator-token`, `local-ops-token`). 전체 운영 API·환경변수는 [README](../README.md#로컬-실행)를 참고하세요.

## 다음 단계

- 아키텍처를 이해하려면 → [Architecture Overview](ARCHITECTURE.md)
- 결정 배경을 보려면 → [ADR Index](adr/README.md)
- 개선 로드맵/백로그 → (작업 진행 중)
