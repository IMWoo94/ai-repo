# ADR-0037: Operator/Admin Token Split

## 상태

Accepted

## 배경

`v0.7.0` 기준 운영 API는 Spring Security role model을 사용하지만, 인증 입력은 하나의 `X-Admin-Token`과 `X-Operator-Id`에 의존했다. 이 방식은 MVP에서는 충분했지만, 운영 조회와 변경성 운영 조치를 구분하기 어렵다.

금융/핀테크 운영에서는 manual review 조회, relay health 조회, access audit 조회 같은 관측 행위와 requeue, operational log pruning 같은 변경성 조치를 분리해야 한다. 특히 requeue와 pruning은 시스템 상태를 바꾸거나 운영 기록을 삭제하므로 더 높은 권한과 감사 기준이 필요하다.

## 선택지

### 선택지 A: 기존 admin token 단일 모델 유지

장점:

- 구현이 단순하다.
- 기존 로컬 시연과 문서 변경이 적다.

단점:

- 조회 운영자와 관리자 조치 권한이 실질적으로 분리되지 않는다.
- operator/admin role model이 Spring Security 내부에서만 존재하고 인증 입력에는 반영되지 않는다.

### 선택지 B: operator token과 admin token을 분리한다

장점:

- 조회성 운영 API와 변경성 운영 API를 명확히 구분할 수 있다.
- 운영자 콘솔에서 어떤 조치가 admin 권한을 요구하는지 드러난다.
- 향후 실제 로그인/권한 체계로 전환할 때 role mapping 근거가 생긴다.

단점:

- 로컬 header contract와 문서가 늘어난다.
- 테스트 matrix가 증가한다.

### 선택지 C: 즉시 실제 로그인/JWT로 전환한다

장점:

- 운영 환경에 가까운 인증 모델을 학습할 수 있다.

단점:

- 현재 MVP의 핵심 목표보다 범위가 크다.
- 사용자/운영자 계정 저장소, token 발급, 만료, refresh, 권한 관리까지 같이 설계해야 한다.

## 결정

선택지 B를 채택한다.

- `X-Operator-Token`은 `ROLE_OPERATOR`를 부여한다.
- `X-Admin-Token`은 `ROLE_OPERATOR`, `ROLE_ADMIN`을 부여한다.
- 두 token 모두 `X-Operator-Id`가 있어야 실제 권한을 가진 인증 객체가 된다.
- manual review 조회, requeue audit 조회, relay health/run 조회, admin access audit 조회는 `ROLE_OPERATOR` 이상을 요구한다.
- manual review requeue와 operational log pruning은 `ROLE_ADMIN`을 요구한다.

## 결과

장점:

- 운영자 화면에서 조회 권한과 관리자 조치 권한의 차이가 드러난다.
- requeue/pruning 같은 변경성 API가 operator token만으로는 실행되지 않는다.
- 401/403 matrix 테스트로 인증 실패와 인가 실패가 분리된다.

비용:

- 로컬 실행과 smoke script가 `X-Operator-Token`을 추가로 알아야 한다.
- 아직 실제 로그인은 아니므로 token 자체는 local header 기반이다.

## 후속 작업

- requeue 승인 워크플로우를 도입할 때 요청자/승인자 권한을 더 세분화한다.
- 실제 로그인/JWT 전환 시 이 ADR의 role mapping을 기준으로 계정 권한 모델을 설계한다.
- admin token 사용 이력을 access audit에서 별도 outcome 또는 role로 남길지 검토한다.
