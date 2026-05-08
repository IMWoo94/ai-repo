# Direct Requeue API Deprecation

## 배경

Requeue approval/rejection workflow가 도입된 뒤에도 기존 직접 requeue API가 성공하면 승인 절차를 우회할 수 있다.

금융/핀테크 운영 관점에서는 requeue 같은 변경성 운영 조치가 요청, 승인, 실행 이력으로 설명되어야 한다.

## 목표

- 기존 직접 requeue HTTP API를 상태 변경 불가로 전환한다.
- deprecated API 호출자는 명확한 오류 코드를 받는다.
- 시나리오 테스트와 문서를 workflow-only 정책으로 갱신한다.

## 완료 기준

- [x] 직접 requeue API가 `410 Gone`을 반환한다.
- [x] 오류 코드는 `DIRECT_REQUEUE_API_DEPRECATED`다.
- [x] 직접 API 호출 후 event 상태와 audit이 변경되지 않는다.
- [x] scenario test가 `REQUESTED -> APPROVED -> EXECUTED` 흐름을 사용한다.
- [x] README, ADR, progress, Wiki, release 후보 문서가 갱신된다.

## 제외

- deprecated endpoint 완전 제거
- 실제 로그인/OIDC 기반 role scope 세분화
