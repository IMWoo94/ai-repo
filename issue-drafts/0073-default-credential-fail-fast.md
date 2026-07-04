# 배포 프로파일에서 기본 JWT secret·운영 토큰 fail-fast

## 배경

`JwtSecretGuard`는 `prod` 프로파일에서만 공개된 기본 JWT secret을 fail-fast로 막았는데, 실제 로컬 k8s 배포는 `postgres` 프로파일로 뜬다(`deploy/k8s/app.yaml`). 그래서 기본 JWT secret과 기본 운영 토큰(`local-ops-token`/`local-operator-token`, `application.yml`)이 배포 환경에서 그대로 동작했다.

## 완료 조건

- [x] 가드 적용 범위를 재정의: 배포 프로파일(`postgres`/`prod`)에서 공개 기본값이면 fail-fast, 그 외 프로파일은 경고.
- [x] 기본 운영 토큰(admin/operator)에도 동일 정책 적용(`OpsTokenGuard`).
- [x] k8s 매니페스트(`deploy/k8s/app.yaml`)에 명시 값 주입 후 회귀 테스트(#121 Secret 이슈와 연계).

## 관련 문서

- `docs/adr/0062-default-credential-fail-fast.md`
- `docs/progress/0073-default-credential-fail-fast.md`
