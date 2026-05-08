# ADR-0038: Requeue Approval Workflow

## 상태

Accepted

## 배경

기존 manual review requeue는 admin token을 가진 운영자가 단일 API 호출로 `MANUAL_REVIEW` outbox event를 `PENDING`으로 되돌렸다. 이 방식은 로컬 MVP에서는 빠르지만, 금융/핀테크 운영 관점에서는 요청자와 승인자가 분리되지 않아 4-eyes 원칙을 설명하기 어렵다.

Requeue는 실패 event를 다시 자동 처리 흐름에 넣는 변경성 운영 조치다. 따라서 “누가 요청했는가”, “누가 승인했는가”, “누가 실행했는가”가 분리되어야 한다.

## 결정

manual review requeue에 승인 요청 워크플로우를 추가한다.

상태 모델:

```text
REQUESTED -> APPROVED -> EXECUTED
REQUESTED -> REJECTED
```

API 계약:

| 단계 | API | 권한 |
| --- | --- | --- |
| 요청 | `POST /api/v1/outbox-events/{outboxEventId}/requeue-requests` | `ROLE_OPERATOR` |
| 조회 | `GET /api/v1/outbox-events/{outboxEventId}/requeue-requests` | `ROLE_OPERATOR` |
| 승인 | `POST /api/v1/outbox-events/requeue-requests/{requestId}/approve` | `ROLE_ADMIN` |
| 실행 | `POST /api/v1/outbox-events/requeue-requests/{requestId}/execute` | `ROLE_ADMIN` |
| 반려 | `POST /api/v1/outbox-events/requeue-requests/{requestId}/reject` | `ROLE_ADMIN` |

승인자와 반려자는 요청자와 달라야 한다.

실행 단계에서만 실제 outbox event를 `PENDING`으로 되돌리고 기존 requeue audit을 저장한다.

반려 단계에서는 outbox event를 변경하지 않고 `MANUAL_REVIEW` 상태를 유지한다. 반려자, 반려 사유, 반려 시각을 request record에 저장한다.

## 트레이드오프

### 장점

- requeue 변경 조치가 요청, 승인, 실행 단계로 분리된다.
- 운영자 화면과 E2E에서 금융권 4-eyes 운영 패턴을 검증할 수 있다.
- 기존 requeue audit은 유지하면서 승인 이력을 별도 구조로 확장한다.

### 비용

- API와 UI 단계가 늘어난다.
- 로컬 header 기반 identity에서는 실제 사용자 인증과 승인자 자격을 완전히 보장하지 못한다.
- 기존 직접 requeue API와 신규 workflow API가 일시적으로 공존한다.

## 검증 기준

- operator token은 requeue 요청을 만들 수 있다.
- operator token만으로 approve/execute는 실패한다.
- 요청자와 동일한 operator id는 승인할 수 없다.
- 요청자와 동일한 operator id는 반려할 수 없다.
- 승인된 요청만 실행할 수 있다.
- 실행 후 outbox event는 `PENDING`으로 돌아가고 requeue audit이 남는다.
- 반려 후 outbox event는 `MANUAL_REVIEW`를 유지하고 requeue audit은 남지 않는다.

## 후속 작업

- 기존 직접 requeue API를 deprecate할지 결정한다.
- 실제 로그인/OIDC 도입 시 requester, approver, executor identity를 인증 주체와 연결한다.
- 반려 후 재요청 정책을 추가할지 검토한다.
