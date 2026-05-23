# ADR-0040: Direct Requeue API Deprecation

## 상태

Accepted

## 배경

manual review requeue는 `REQUESTED -> APPROVED -> EXECUTED`와 `REQUESTED -> REJECTED` workflow로 확장되었다.

그런데 기존 `POST /api/v1/outbox-events/{outboxEventId}/requeue` API가 계속 성공하면 admin 한 명이 요청, 승인, 실행을 우회할 수 있다. 이는 금융/핀테크 운영에서 필요한 4-eyes 원칙과 감사 설명력을 약하게 만든다.

## 결정

HTTP 직접 requeue API는 deprecated 상태로 유지하되 더 이상 상태를 변경하지 않는다.

- `POST /api/v1/outbox-events/{outboxEventId}/requeue`는 `410 Gone`을 반환한다.
- 응답 코드는 `DIRECT_REQUEUE_API_DEPRECATED`다.
- 사용자는 requeue request workflow API를 사용해야 한다.
- 내부 repository/service의 requeue primitive는 workflow execute 단계에서 필요한 구현 세부로 유지한다.

## 트레이드오프

### 장점

- 운영자가 승인 workflow를 우회할 수 없다.
- 기존 API 경로를 호출하는 클라이언트가 명확한 deprecation 오류를 받는다.
- 감사 이력은 request, approval/rejection, execution 모델로 일관된다.

### 비용

- 기존 직접 API를 쓰던 로컬 스크립트나 테스트는 workflow API로 전환해야 한다.
- endpoint 자체는 호환성 오류 응답을 위해 한동안 남는다.
- 내부 primitive와 외부 API 정책이 분리되어 문서 설명이 필요하다.

## 검증 기준

- admin token으로 직접 requeue API를 호출하면 `410 Gone`과 `DIRECT_REQUEUE_API_DEPRECATED`가 반환된다.
- 직접 requeue API 호출 후 outbox event는 `MANUAL_REVIEW` 상태를 유지한다.
- 직접 requeue API 호출 후 requeue audit은 생성되지 않는다.
- scenario test는 요청, 승인, 실행 workflow로 manual review event를 `PENDING`으로 되돌린다.

## 후속 작업

- 다음 major API 정리 시 deprecated endpoint 제거 여부를 결정한다.
- 실제 identity 도입 후 workflow 단계별 role scope를 더 세분화한다.
