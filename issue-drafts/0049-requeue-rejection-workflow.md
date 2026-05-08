# Requeue Rejection Workflow

## 배경

Requeue approval workflow는 요청, 승인, 실행을 분리했지만 승인하지 않는 결정도 운영 기록으로 남겨야 한다.

## 목표

- `REQUESTED -> REJECTED` 상태 전이를 추가한다.
- 반려자, 반려 사유, 반려 시각을 저장한다.
- 반려 시 outbox event는 `MANUAL_REVIEW` 상태를 유지한다.

## 완료 기준

- [x] reject API가 있다.
- [x] operator token만으로 reject는 실패한다.
- [x] 요청자와 같은 operator id는 reject할 수 없다.
- [x] reject 후 outbox event는 `MANUAL_REVIEW` 상태를 유지한다.
- [x] reject 후 requeue audit은 남지 않는다.
- [x] frontend unit/E2E에서 반려 흐름을 검증한다.

## 제외

- 반려 후 자동 재요청 정책
- 실제 로그인/OIDC identity
