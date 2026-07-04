# ADR-0062: 배포 프로파일에서 기본 자격증명 fail-fast

## 상태

Accepted

## 배경

`JwtSecretGuard`는 리포지토리에 공개된 기본 JWT secret(`local-dev-jwt-secret-please-change-32b`)이 남아 있으면 애플리케이션 기동을 실패시켰지만, 그 판정을 `prod` 프로파일에서만 했다. 그런데 실제 로컬 k8s 배포는 `postgres` 프로파일로 뜬다(`deploy/k8s/app.yaml`의 `SPRING_PROFILES_ACTIVE=postgres`). 결과적으로 배포 환경에서 기본 JWT secret은 물론, 마찬가지로 공개된 기본 운영 토큰(`local-ops-token`/`local-operator-token`, `application.yml`)도 그대로 살아남았다. 공개된 기본 secret으로 서명된 JWT는 임의 회원으로 위조할 수 있고, 공개된 기본 운영 토큰은 누구에게나 operator/admin 접근을 내준다.

## 결정

| 항목 | 결정 |
| --- | --- |
| 배포 프로파일 정의 | `postgres` 또는 `prod`가 활성일 때를 "배포 런타임"으로 본다(`DeployedProfiles`). local/test/프로파일 없음은 개발 환경으로 취급 |
| JWT secret 가드 | 배포 프로파일 + 값이 공개 기본값이면 기동 실패, 그 외에는 경고 로그만(기존 정책을 `prod`→`postgres/prod`로 확장) |
| 운영 토큰 가드 | 같은 정책을 `ai-repo.ops.admin-token`/`operator-token`에도 적용하는 `OpsTokenGuard` 추가 |
| override 경로 | 배포 시 `AI_REPO_AUTH_JWT_SECRET`, `AI_REPO_OPS_ADMIN_TOKEN`, `AI_REPO_OPS_OPERATOR_TOKEN`를 명시 주입 |
| k8s 매니페스트 | `deploy/k8s/app.yaml`에 위 세 env를 비-기본 로컬 값으로 명시 주입해 배포가 계속 기동하도록 함. 원격 전환 시 Secret 주입으로 대체(#121) |

## 트레이드오프

### 장점

- 배포 환경에서 공개된 기본 자격증명이 절대 유효하지 않도록 강제한다(fail-fast).
- JWT secret과 운영 토큰이 동일한 가드 패턴/프로파일 정책을 공유해 일관성이 있다.
- 로컬/테스트 실행은 기본값으로 계속 동작해 개발 마찰이 없다.

### 비용

- `deploy/k8s/app.yaml`이 여전히 평문 비-기본 값을 담는다(로컬 학습 랩 전용). 실제 비밀 관리는 #121의 Secret 주입에서 완결된다.
- "배포 프로파일" 목록(`postgres`/`prod`)이 하드코딩이라, 새 배포 프로파일을 추가하면 `DeployedProfiles`도 갱신해야 한다.

## 대안

- opt-in env 하나로 가드를 켜는 방식: 명시 opt-in을 깜빡하면 다시 무방비가 되므로, 배포 프로파일 기준 fail-fast(안전 기본값)를 택했다.
- 모든 프로파일에서 fail-fast: local/test 기동이 깨지고 CI 픽스처가 번거로워진다.
