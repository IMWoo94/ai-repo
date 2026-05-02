# ADR-0028: Admin API Authz Boundary

## 상태

Accepted

## 맥락

Outbox manual review와 requeue API는 운영자가 실패 event를 조회하고 재처리하는 운영 행위다. 이 API가 공개 상태로 남아 있으면 누구나 실패 event를 조회하거나 재처리할 수 있고, requeue audit의 operator도 요청 body에 의존하게 된다.

현재 프로젝트는 포트폴리오형 학습 단계이며 사용자 로그인, OAuth/OIDC, 사내 IAM은 아직 없다. 따라서 실제 인증 시스템을 바로 붙이기보다 운영 API 경계를 먼저 코드와 테스트로 고정해야 한다.

## 선택지

### 선택지 A: 인증/인가 없이 유지한다

장점:

- 로컬 테스트와 수동 호출이 가장 쉽다.
- Spring Security나 인증 설정이 필요 없다.

단점:

- 운영 API와 사용자 API의 위험도가 구분되지 않는다.
- requeue 같은 운영 행위가 아무 호출자에게나 열려 있다.
- 감사 이력의 operator 신뢰성이 낮다.

### 선택지 B: 공유 admin token과 operator header를 사용한다

장점:

- 의존성 추가 없이 운영 API 보호 경계를 빠르게 만들 수 있다.
- 인증 실패와 권한 실패를 `401`, `403`으로 구분할 수 있다.
- requeue audit operator를 request body가 아니라 인증된 header에서 가져올 수 있다.

단점:

- 공유 token은 회전, 만료, 사용자별 권한 분리가 약하다.
- 실제 금융권 운영 인증 모델을 대체할 수 없다.
- header 기반이므로 gateway/TLS/secret 관리가 전제되어야 한다.

### 선택지 C: Spring Security + JWT/OIDC를 바로 도입한다

장점:

- 운영 시스템에 가까운 인증/인가 모델을 설계할 수 있다.
- 사용자 인증과 운영자 권한을 같은 프레임워크에서 확장할 수 있다.

단점:

- 현재 단계에서는 IAM, token issuer, role model이 아직 정의되지 않았다.
- 학습 범위가 outbox 운영 흐름에서 인증 인프라 구축으로 커진다.
- 기존 API 테스트 전체에 보안 설정 영향이 생긴다.

## 결정

공유 admin token과 operator header를 사용한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 보호 대상 | `/api/v1/outbox-events/**` 운영 API |
| 인증 header | `X-Admin-Token` |
| 운영자 header | `X-Operator-Id` |
| 로컬 기본 token | `local-ops-token` |
| 환경 변수 override | `AI_REPO_OPS_ADMIN_TOKEN` |
| 인증 실패 | `401 ADMIN_AUTHENTICATION_REQUIRED` |
| 인가 실패 | `403 ADMIN_AUTHORIZATION_DENIED` |
| 감사 operator | `X-Operator-Id` 값을 사용 |
| token 비교 | 상수시간 비교 |

## 결과

장점:

- 사용자 지갑 API는 그대로 두고 운영 API만 명확히 보호한다.
- manual review 조회, requeue, requeue audit 조회가 같은 권한 정책을 사용한다.
- requeue 감사 이력의 operator가 요청 body 조작에 의존하지 않는다.

비용:

- 공유 token은 운영급 인증이 아니므로 실제 서비스에서는 반드시 교체해야 한다.
- 운영자별 role, 권한 scope, token 만료, 회전 정책은 아직 없다.

후속 작업:

- Spring Security 기반 `ROLE_OPERATOR`, `ROLE_ADMIN` 모델을 검토한다.
- 운영자 승인 workflow와 requeue 2인 승인 정책을 설계한다.
- 운영 API 접근 로그를 별도 audit event로 남긴다.
