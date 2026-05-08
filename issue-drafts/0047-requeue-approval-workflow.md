# Requeue Approval Workflow

## 배경

manual review requeue는 실패 outbox event를 다시 자동 처리 흐름에 넣는 변경성 운영 조치다.

현재는 admin token을 가진 운영자가 단일 API로 실행할 수 있어 요청자/승인자 분리와 4-eyes 운영 원칙을 설명하기 어렵다.

## 목표

- requeue 요청, 승인, 실행 상태를 분리한다.
- 상태 모델은 `REQUESTED -> APPROVED -> EXECUTED`를 사용한다.
- 승인자는 요청자와 달라야 한다.
- 실제 outbox event 상태 변경과 requeue audit 저장은 execute 단계에서만 수행한다.

## 범위

- `operation_outbox_requeue_requests` persistence 추가
- request/approve/execute API 추가
- Spring Security 권한 경계 적용
- 운영자 콘솔 UI 변경
- backend, frontend, E2E 테스트 갱신
- ADR/progress/Wiki/release 문서 갱신

## 완료 기준

- [x] operator token으로 requeue 요청을 만들 수 있다.
- [x] operator token만으로 approve/execute는 실패한다.
- [x] 요청자와 동일한 operator id는 승인할 수 없다.
- [x] 승인된 요청만 실행할 수 있다.
- [x] execute 후 outbox event는 `PENDING`으로 바뀌고 requeue audit이 남는다.
- [x] 프론트 E2E가 요청, 승인, 실행, audit trail을 검증한다.

## 제외

- 실제 로그인/OIDC identity
- 기존 직접 requeue API deprecation

## 후속 반영

- 승인 반려 상태는 `0049-requeue-rejection-workflow.md`에서 추가했다.
