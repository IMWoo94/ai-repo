# QA Scenarios

이 문서는 1차 MVP를 사람이 검증할 때 사용할 QA 시나리오 초안이다.

자동화된 source of truth는 테스트 코드와 `docs/testing/local-test-guide.md`이며, 이 Wiki 초안은 포트폴리오형 설명과 수동 시연 순서를 정리한다.

## MVP 핵심 시나리오

| 시나리오 | 확인 내용 | 자동화 근거 |
| --- | --- | --- |
| 초기 조회 | `wallet-001` 잔액과 거래내역이 화면에 표시된다 | Frontend E2E |
| 충전 성공 | 충전 후 잔액이 증가하고 최근 operation 증거가 표시된다 | Frontend E2E, scenario test |
| 송금 성공 | 출금 지갑 잔액이 감소하고 outbox event가 표시된다 | Frontend E2E, scenario test |
| 잔액 부족 실패 | 잔액 부족 송금은 오류 메시지를 표시하고 잔액을 바꾸지 않는다 | Frontend E2E, API test |
| 운영자 콘솔 empty | manual review event가 없으면 empty state를 표시한다 | Frontend unit, E2E |
| 운영자 인증 오류 | 잘못된 admin token은 `ADMIN_AUTHENTICATION_REQUIRED`로 표시된다 | Frontend E2E, API test |
| 운영자 requeue | manual review event를 requeue하면 audit trail이 남는다 | Frontend unit, scenario test |
| PostgreSQL runtime | `postgres` profile에서 Flyway migration과 대표 흐름이 동작한다 | PostgreSQL scenario CI |

## 수동 시연 순서

1. 백엔드를 실행한다.
   - `./gradlew bootRun --no-daemon`
2. 프론트를 실행한다.
   - `npm --prefix frontend run dev -- --host 127.0.0.1`
3. 브라우저에서 `http://127.0.0.1:5173`을 연다.
4. 초기 잔액 `125,000 KRW`를 확인한다.
5. 충전 금액을 입력하고 `충전하기`를 실행한다.
6. 잔액, 거래내역, 원장, 감사 로그, operation 증거가 같이 바뀌는지 확인한다.
7. 송금 금액을 입력하고 `송금하기`를 실행한다.
8. 잔액 부족 송금으로 오류 상태를 확인한다.
9. 운영자 콘솔에서 local admin token으로 manual review 조회를 실행한다.
10. 잘못된 admin token으로 인증 오류 상태를 확인한다.

## 릴리스 후보 gate

릴리스 후보는 다음 명령과 CI job이 통과해야 한다.

- `./gradlew check`
- `./gradlew scenarioTest`
- `./gradlew postgresScenarioTest`
- `npm --prefix frontend run test`
- `npm --prefix frontend run build`
- `npm --prefix frontend run e2e`
- `git diff --check`

## 남은 QA 후보

- manual review event fixture 기반 requeue 성공 E2E
- relay health/pruning 운영자 화면 E2E
- release smoke script 또는 actuator health check
- broker-specific Testcontainers contract scenario
