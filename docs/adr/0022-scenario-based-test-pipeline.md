# ADR-0022: Scenario-Based Test Pipeline

## 상태

Accepted

## 맥락

v0.6.0 기준선은 단위 테스트, API 테스트, JDBC/Testcontainers 테스트, GitHub Actions `Gradle Check`를 갖췄다. 하지만 현재 CI는 `./gradlew check` 하나로 묶여 있어 테스트 실패가 “계층 테스트 실패”인지 “사용자 흐름 실패”인지 구분하기 어렵다.

핀테크 학습 프로젝트에서는 개별 정책뿐 아니라 다음 질문에도 답해야 한다.

- 사용자가 잔액 확인 후 충전/송금하면 결과 증거가 모두 남는가?
- 멱등 재시도는 사용자 응답과 내부 기록 모두에서 중복을 막는가?
- outbox 실패가 manual review와 requeue 감사 이력으로 이어지는가?
- QA가 기능 단위가 아니라 업무 흐름 단위로 검증할 수 있는가?

## 선택지

### 선택지 A: 기존 `check`만 유지한다

장점:

- CI가 단순하다.
- 실행 시간이 가장 짧다.

단점:

- 사용자/운영 흐름의 회귀를 별도 이름으로 추적하기 어렵다.
- 테스트 실패 원인 분류가 늦어진다.

### 선택지 B: `scenarioTest` Gradle task와 CI job을 분리한다

장점:

- 일반 회귀 테스트와 시나리오 흐름 검증을 분리해 볼 수 있다.
- QA와 릴리스 관리자가 “대표 흐름 통과 여부”를 직접 확인할 수 있다.
- 향후 smoke test, broker adapter, 인증/인가 흐름을 별도 게이트로 확장하기 쉽다.

단점:

- CI job이 하나 늘어난다.
- 시나리오 테스트가 많아지면 실행 시간이 늘어날 수 있다.

### 선택지 C: 별도 E2E 도구를 바로 도입한다

장점:

- 실제 사용자 환경에 가까운 검증이 가능하다.
- 화면/API/배포 검증까지 확장하기 좋다.

단점:

- 현재 서버 API 중심 학습 단계에 비해 도입 비용이 크다.
- 인증/인가, 배포 환경, 테스트 데이터 관리가 먼저 필요하다.

## 결정

`scenarioTest` Gradle task와 CI job을 분리한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| JUnit tag | `scenario` |
| Gradle task | `./gradlew scenarioTest` |
| 일반 test | `scenario` tag 제외 |
| CI job | `Gradle Check`, `Scenario Test` 분리 |
| 첫 시나리오 | 돈 이동 증거 흐름, outbox manual review/requeue 감사 흐름 |
| 범위 제외 | 브라우저 E2E, 실제 배포 smoke test, 외부 broker |

## 결과

장점:

- 릴리스 전에 대표 업무 흐름을 별도 게이트로 확인할 수 있다.
- 테스트 실패가 계층 회귀인지 시나리오 회귀인지 구분된다.
- QA 시나리오가 코드와 문서에 함께 남는다.

비용:

- 시나리오 테스트 데이터와 실행 시간을 관리해야 한다.
- 모든 테스트를 시나리오로 만들면 중복이 늘어나므로 핵심 흐름만 선별해야 한다.

후속 작업:

- PostgreSQL profile 기반 scenario test를 별도 tag로 분리할지 결정한다.
- 실제 broker adapter 도입 후 outbox publish scenario를 추가한다.
- 인증/인가 도입 후 관리자 requeue 권한 scenario를 추가한다.
