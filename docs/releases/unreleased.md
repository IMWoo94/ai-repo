# Unreleased Release Candidate Notes

## 릴리스 성격

`unreleased`는 `v0.7.0` 이후 `main`에 누적될 변경 후보를 추적한다.

이 문서는 GitHub Release가 아니라, 다음 실제 tag를 만들기 전 현재 개발분을 정리하는 staging 문서다.

## 후보 범위

- CI에 `Dev Rules Check` job을 추가해 코드/DB/프론트/CI 변경 시 문서와 테스트 동기화 누락을 빠르게 탐지한다.
- macOS 기본 Bash에서도 `scripts/check-dev-rules.sh`가 동작하도록 배열 입력 처리를 POSIX 친화적인 루프로 구성한다.
- `AdminAuthorizationGuard` 단위 테스트를 추가해 admin token과 operator id 검증 정책을 controller test보다 작은 범위에서 고정한다.

## MVP 출시 판단 기준

다음 release 후보는 다음 조건을 만족해야 한다.

- 핵심 사용자 흐름인 잔액 조회, 충전, 송금, 거래내역 확인이 화면과 API에서 동작한다.
- 돈 이동 결과가 원장, 감사 로그, operation step log, outbox event로 추적된다.
- 운영자는 manual review outbox를 화면에서 조회하고, requeue와 audit trail을 확인할 수 있다.
- 운영 API는 local admin token과 operator id로 보호된다.
- PostgreSQL profile이 Flyway migration과 Testcontainers scenario로 검증된다.
- 프론트 build, unit, E2E가 CI에서 분리 검증된다.
- 백엔드 unit/API, scenario, PostgreSQL scenario가 CI에서 분리 검증된다.
- README, ADR, progress, issue draft, local test guide가 현재 상태와 모순되지 않는다.

## 검증 게이트

릴리스 후보 PR 또는 tag 전에는 다음 명령을 통과해야 한다.

```bash
./gradlew check
./gradlew scenarioTest
./gradlew postgresScenarioTest
scripts/check-dev-rules.sh
npm --prefix frontend run test
npm --prefix frontend run build
npm --prefix frontend run e2e
scripts/mvp-local-smoke.sh
docker compose config
git diff --check
```

GitHub Actions에서는 다음 job이 통과해야 한다.

- `Dev Rules Check`
- `Gradle Check`
- `Scenario Test`
- `PostgreSQL Scenario Test`
- `Frontend Unit Test`
- `Frontend Build`
- `Frontend E2E`

## 알려진 제약

- 운영자 requeue 성공 E2E는 아직 manual review fixture 생성 정책이 필요하다.
- 운영자 relay health와 pruning 화면은 아직 없다.
- 운영자 승인 워크플로우와 external alert channel은 아직 없다.
- admin token과 operator identity는 local header 기반이며 실제 로그인과 분리되어 있다.
- Kafka/RabbitMQ/SQS 같은 broker-specific adapter는 아직 없다.
- GitHub Wiki 동기화는 아직 수동 후보 문서 수준이다.

## 출시 전 blocker

- 다음 release 범위가 확정되면 버전 번호와 tag 정책을 결정한다.
- `unreleased` 내용을 실제 버전 릴리스 노트로 승격한다.
- `scripts/mvp-local-smoke.sh` 실행 결과를 릴리스 PR에 첨부한다.

## 후속 후보

- 운영자 requeue full E2E fixture
- relay health/pruning operator UI
- broker-specific Testcontainers contract
- consumer idempotency
- external alert channel
