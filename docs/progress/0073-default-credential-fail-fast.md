# 0073 배포 프로파일 기본 자격증명 fail-fast

## 스펙 목표

`postgres` 프로파일(실제 로컬 k8s 배포)에서 공개된 기본 JWT secret과 기본 운영 토큰(admin/operator)이 그대로 동작하던 결함(#122)을 닫는다.

## 완료 결과

- `DeployedProfiles`를 추가해 배포 런타임(`postgres` 또는 `prod`)을 한 곳에서 판정한다.
- `JwtSecretGuard`의 fail-fast 범위를 `prod`에서 배포 프로파일(`postgres`/`prod`)로 확장했다.
- `OpsTokenGuard`를 추가해 `ai-repo.ops.admin-token`/`operator-token`에도 동일 정책(배포 프로파일 + 공개 기본값 → 기동 실패, 그 외 경고)을 적용했다.
- `deploy/k8s/app.yaml`에 `AI_REPO_AUTH_JWT_SECRET`, `AI_REPO_OPS_ADMIN_TOKEN`, `AI_REPO_OPS_OPERATOR_TOKEN`를 비-기본 로컬 값으로 명시 주입해 배포가 계속 기동하도록 했다.

## 검증

- `JwtSecretGuardTest`: prod/postgres에서 기본 secret이면 fail-fast, override면 통과, 비배포 프로파일에서는 기본값 허용.
- `OpsTokenGuardTest`: prod/postgres에서 기본 admin/operator 토큰이면 fail-fast, override면 통과, 비배포 프로파일에서는 기본값 허용.
- `./gradlew test` 통과.

## 남은 일

- 실제 비밀 관리는 #121의 k8s Secret 주입으로 `app.yaml` 평문 값을 대체한다.
- 새 배포 프로파일 도입 시 `DeployedProfiles` 목록 갱신 필요.
