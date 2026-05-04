# Current Development Baseline Review

## 기준

- 기준 버전: `v0.6.0`
- 기준 브랜치: `main`
- 검토 관점: 코드 품질, 안정성, 테스트, 문서, 릴리스 운영

## 강점

- 도메인 흐름이 단계적으로 누적되어 있다. 잔액/거래내역, 회원/지갑, 충전/송금, 원장/감사, outbox 운영 흐름이 문서와 코드에 연결되어 있다.
- 중요한 결정이 ADR로 남아 있다. 특히 멱등성, PostgreSQL lock, Flyway, outbox relay, manual review가 추적 가능하다.
- 테스트가 계층별로 존재한다. 도메인 객체, API, service, JDBC, Testcontainers 검증이 분리되어 있다.
- 릴리스 기준선이 생겼다. `v0.6.0` tag와 GitHub Release로 검증 가능한 상태를 재현할 수 있다.

## 주요 개선점

| 우선순위 | 항목 | 이유 | 제안 |
| --- | --- | --- | --- |
| P1 | 시나리오 기반 게이트 부족 | 개별 테스트는 충분하지만 대표 업무 흐름 통과 여부가 CI에서 별도로 보이지 않는다 | `scenarioTest` task와 CI job을 분리한다 |
| P2 | 운영 API 인증/인가 부재 | manual review/requeue API는 운영 행위인데 현재 누구나 호출 가능한 형태다 | 관리자 인증/인가 ADR과 테스트를 추가한다 |
| P2 | 실제 broker 미연동 | outbox는 상태와 retry는 있지만 외부 publish adapter가 없다 | broker port/interface와 fake adapter부터 도입한다 |
| P3 | Wiki 동기화 수동 | `scripts/sync-wiki-drafts.sh`로 checkout 동기화 절차는 생겼고 실제 push는 release operation이다 | 릴리스 체크리스트에서 Wiki push를 확인한다 |
| P3 | release health check 부재 | Release는 발행됐지만 로컬 앱 기동 smoke 검증은 별도 게이트가 아니다 | `bootRun` 또는 actuator 도입 후 smoke script를 추가한다 |

## 이번 작업에서 반영한 개선

- P1 항목인 시나리오 기반 게이트 부족을 `scenarioTest` Gradle task와 GitHub Actions `Scenario Test` job으로 보완했다.
- 돈 이동 증거 흐름과 outbox 운영 흐름을 첫 시나리오 테스트로 고정했다.
- 남은 개선점은 실제 Wiki push, release health check, broker-specific 검증, 운영자 승인 workflow다.

## 테스트 파이프라인 진단

현재 상태:

- `./gradlew test`: 계층 테스트 중심
- `./gradlew check`: Gradle 기본 verification
- GitHub Actions: `Gradle Check` 단일 job

개선 방향:

- 일반 테스트와 시나리오 테스트를 JUnit tag로 분리한다.
- CI에서 `Gradle Check`와 `Scenario Test`를 별도 job으로 표시한다.
- 시나리오 테스트는 기능 하나가 아니라 사용자/운영 흐름 하나를 검증한다.

## 권장 다음 순서

1. 시나리오 테스트 파이프라인 구축
2. 실제 broker adapter 추상화
3. 관리자 인증/인가 정책
4. requeue 승인 워크플로우
5. release smoke test와 Wiki push 절차 실행
