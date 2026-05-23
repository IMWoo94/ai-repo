# 2026-05-22 Operational Alert Suppression and Pruning Codex Review

## 범위

- `DESIGN-apple.md` 추가
- `OperationalAlertPolicy`
- operational alert suppression window
- operational alert pruning
- JDBC/InMemory repository 변경
- service/controller/scheduler/JDBC 테스트와 문서 동기화

## 수용

1. Suppression check/insert 경합
   - 리뷰 의견: 기존 구현은 `exists` 확인 뒤 별도 insert를 수행해 동시 health 조회에서 같은 alert가 중복 저장될 수 있다.
   - 반영: `OperationalAlertService.publishHealthAlert`를 단일 애플리케이션 인스턴스 안에서 직렬화하고 동시 publish 테스트를 추가했다.

2. Out-of-order timestamp suppression
   - 리뷰 의견: suppression lookup이 `since` 하한만 사용해 미래 alert가 과거 alert를 억제할 수 있다.
   - 반영: repository contract를 `existsOperationalAlertBetween`으로 바꾸고 `occurredAt` 상한을 포함했다. 과거 alert가 미래 alert 때문에 억제되지 않는 테스트를 추가했다.

## 반박

없음.

## 후속 과제

- Suppression lookup용 복합 인덱스 추가 검토
- 다중 애플리케이션 인스턴스 환경의 DB 원자 suppression 정책 결정
- Slack/Webhook adapter와 contract test
- pruning 실행 이력 저장과 조회 API
- 운영 alert 화면 연결

## 블로커

리뷰 시점에는 동시 요청 중복 저장이 블로커로 지적되었다. 수용 항목 반영 후 재검증 대상으로 전환했다.

## 최종 판단

수용 항목 반영 후 관련 테스트와 `scripts/check-dev-rules.sh`를 재실행한다.
