# Code Review — Operational Alert Record Channel

## 범위

- 브랜치: `feature/98-top3-operational-hardening`
- 변경 요약: relay/consumer health warning·critical 판정을 `operational_alerts` record로 저장하고 조회 API를 추가한다.
- 리뷰 방식: Codex 백엔드 리뷰어, Codex QA/문서 리뷰어

## Codex 리뷰

### 수용

- 운영 alert API가 operator token 보호와 admin access audit 대상에 포함되어 있다.
- `OK`, `NO_DATA`는 저장하지 않고 `WARNING`, `CRITICAL`만 저장하는 방향이 ADR과 맞다.
- service/API/JDBC 테스트와 ADR/progress/release/Wiki 문서가 추가되어 구현 근거가 남는다.

### 반박

- 없음.

### 후속 과제

- alert dedupe/suppression window를 추가한다.
- alert record pruning 정책을 추가한다.
- Slack/Webhook push adapter와 contract test를 추가한다.

## 최종 조치

- 반영: alert 조회 인덱스, `TEXT` reasons 컬럼, health 발행 가드, limit/audit/service 테스트를 추가했다.
- 검증: 관련 테스트와 전체 검증을 실행한다.
- 남은 위험: health 조회가 alert 발행 트리거인 구조는 ADR-0052의 명시된 trade-off로 유지한다.
