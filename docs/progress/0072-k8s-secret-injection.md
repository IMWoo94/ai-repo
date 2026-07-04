# 0072. k8s 자격증명 Secret 주입 전환

## 스펙 목표

- `deploy/k8s`의 평문 DB 비밀번호·운영 토큰 env를 k8s Secret 리소스로 옮긴다.
- Deployment는 `secretKeyRef`로 자격증명을 참조하고, 비밀이 아닌 env는 평문을 유지한다.
- GitOps overlay(`deploy/gitops`)에서 동일하게 렌더된다.
- 가이드 문서의 접속 정보 표·문구를 자격증명 출처(secrets.yaml) 기준으로 갱신한다.

## 완료 결과

- **Secret 신설**(`deploy/k8s/secrets.yaml`): `Secret ai-repo-credentials`(Opaque, `stringData`) — 로컬 학습 전용 고정값. 키: `POSTGRES_DB/USER/PASSWORD`, `AI_REPO_POSTGRES_USERNAME/PASSWORD`, `AI_REPO_OPS_ADMIN_TOKEN`, `AI_REPO_OPS_OPERATOR_TOKEN`, `AI_REPO_AUTH_JWT_SECRET`. env 이름은 `application.yml` 바인딩과 일치. 로컬 전용·원격 별도 주입을 주석으로 명시.
- **참조 전환**: `postgres.yaml`(3개)·`app.yaml`(5개) 자격증명 env를 `valueFrom.secretKeyRef`로 변경. URL·프로파일·스케줄러 플래그 등 비밀 아닌 env는 평문 유지.
- **kustomization**: `deploy/k8s/kustomization.yaml`에 `secrets.yaml` 등록(namespace 다음). `deploy/gitops`는 `../k8s` base를 참조하므로 자동 반영.
- **문서**: `docs/development/k8s-local-monitoring.md` 접속 정보 표(앱 API 토큰·Postgres 행)·구성 목록·보안 주의·토큰 override 문구를 secrets.yaml 출처 기준으로 갱신. ADR-0061 기록.

## 개선 건수

1. `deploy/k8s` 평문 자격증명 env 8종을 `Secret ai-repo-credentials`의 `secretKeyRef`로 전환.
2. 자격증명 출처를 `secrets.yaml` 한 곳으로 일원화하고 가이드 문서 접속 정보를 갱신.

## 검증

- `kubectl --context docker-desktop kustomize deploy/k8s > /dev/null` 렌더 성공(라이브 클러스터 미적용).
- `kubectl kustomize deploy/gitops > /dev/null` 렌더 성공. 렌더 결과에 `Secret ai-repo-credentials`와 `secretKeyRef` 8개 확인.
- `scripts/check-dev-rules.sh`(`AI_REPO_DEV_RULES_BASE=origin/main`) PASS.

## 남은 일

- 원격/스테이징 전환 시 Secret 주입 경로(External Secrets/SealedSecrets 등)와 local/prod-ready overlay 분리 결정(#116 Human Verify).

## 관련 문서

- GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/121
- `docs/adr/0061-k8s-secret-injection.md`
- `docs/development/k8s-local-monitoring.md`
