# ADR-0034: Spring Security Role Model

## 상태

Accepted

## 맥락

운영 API는 `X-Admin-Token`과 `X-Operator-Id` 기반 controller guard로 보호되어 있었다. 이 방식은 빠르게 운영 API를 잠그고 401/403 응답을 고정하는 데 충분했다. 하지만 API별 역할 차이를 표현하기 어렵고, 인증/인가 경계가 Spring Security 표준 filter chain과 분리되어 있었다.

금융/핀테크 운영 API는 위험도가 다르다. Manual review 조회와 requeue는 운영자 작업이고, operational log pruning은 운영 기록 삭제에 가까운 관리자 작업이다. 따라서 header contract는 유지하되, 내부 권한 판단은 Spring Security role model로 이동한다.

## 선택지

### 선택지 A: 기존 controller guard를 유지한다

장점:

- 변경 범위가 작다.
- 기존 테스트와 오류 응답을 그대로 유지하기 쉽다.
- Spring Security 의존성이 필요 없다.

단점:

- controller마다 guard 호출이 반복된다.
- `ROLE_OPERATOR`, `ROLE_ADMIN` 같은 역할 모델을 표현하기 어렵다.
- 향후 JWT/OIDC나 method security로 확장하기 어렵다.

### 선택지 B: header contract는 유지하고 Spring Security filter chain으로 전환한다

장점:

- 기존 로컬 사용법과 API 테스트를 크게 바꾸지 않는다.
- 인증은 `X-Admin-Token`, 운영자 식별은 `X-Operator-Id`로 유지한다.
- 내부 권한은 `ROLE_OPERATOR`, `ROLE_ADMIN`으로 표현할 수 있다.
- 보안 오류 응답을 Spring Security entry point/access denied handler로 통제할 수 있다.

단점:

- Spring Security dependency와 filter chain 설정이 추가된다.
- header token 하나가 현재는 `ROLE_OPERATOR`, `ROLE_ADMIN`을 모두 부여하므로 다중 token/계정 모델은 아직 없다.
- controller guard보다 filter ordering을 더 주의해야 한다.

### 선택지 C: JWT/OIDC를 바로 도입한다

장점:

- 실제 서비스 인증/인가 구조에 가깝다.
- role, scope, principal, token 만료를 표준적으로 다룰 수 있다.

단점:

- 현재 로컬 포트폴리오 단계에서는 IAM, login, token 발급 범위가 과하다.
- 핵심 목표인 운영 API 역할 경계 고정보다 외부 인증 인프라 작업이 커진다.

## 결정

header contract는 유지하고 Spring Security filter chain으로 전환한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 인증 header | `X-Admin-Token` |
| principal header | `X-Operator-Id` |
| role | `ROLE_OPERATOR`, `ROLE_ADMIN` |
| 운영 조회/재처리 API | `ROLE_OPERATOR` |
| operational log pruning API | `ROLE_ADMIN` |
| 세션 | stateless |
| CSRF/form login/http basic | 비활성화 |
| 인증 실패 | `401 ADMIN_AUTHENTICATION_REQUIRED` |
| 인가 실패 | `403 ADMIN_AUTHORIZATION_DENIED` |
| admin access audit | security 401/403도 기록 |

## 결과

장점:

- 운영 API 보호가 controller guard가 아니라 Spring Security filter chain에서 적용된다.
- controller는 비즈니스 동작에 집중하고 보안 판단은 설정으로 이동한다.
- 향후 JWT/OIDC, 다중 token, role 분리로 확장할 수 있는 기반이 생겼다.

비용:

- 현재 로컬 token은 `ROLE_OPERATOR`, `ROLE_ADMIN`을 모두 부여한다.
- 실제 사용자/권한 저장소는 아직 없다.
- 외부 IAM 연동은 후속 작업이다.

후속 작업:

- operator/admin token을 분리할지 결정한다.
- JWT/OIDC 전환 시 principal, role, audit operator를 연결한다.
- pruning API를 실제 운영에서는 더 강한 승인 워크플로우와 연결한다.
