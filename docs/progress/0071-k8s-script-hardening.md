# 0071. k8s 로컬 스크립트 하드닝 (loki/alloy 대기 + ArgoCD 버전 고정)

## 스펙 목표

- `scripts/k8s-local-up.sh` rollout 대기 대상을 kustomization 전체(loki/alloy 포함)로 맞추고 Loki 기동을 스모크로 확인한다.
- `scripts/argocd-install.sh`가 이동하는 `stable` 태그 대신 고정 버전 매니페스트를 apply해 설치 재현성을 확보한다.

## 완료 결과

- **k8s-local-up 대기 확장**: `deployment/loki`, `deployment/alloy` rollout status 대기를 추가(기존 postgres/app/prometheus/grafana에 이어). kustomization에 선언된 6개 Deployment 전체를 대기한다.
- **Loki 스모크**: rollout 완료 후 `svc/loki` 3100 port-forward → `curl -sf http://localhost:3100/ready`(2초 간격 최대 10회 재시도), 성공/실패 모두 port-forward 프로세스를 정리(trap EXIT + 명시적 kill). 기존 스크립트의 `set -euo pipefail`/에러 처리 스타일을 유지.
- **ArgoCD 버전 고정**: `scripts/argocd-install.sh`에 `ARGOCD_VERSION=v3.4.4`를 도입하고 `.../argo-cd/${ARGOCD_VERSION}/manifests/install.yaml`을 apply. 이동하는 `stable` 태그 제거. 버전 갱신은 변수 한 줄만 바꾼다.
- **문서**: `docs/development/ci-cd-gitops.md` 설치 커맨드/설명 갱신, ADR-0059 결정 표에 rollout 대기 범위·ArgoCD 버전 고정 반영.

## 개선 건수

1. k8s-local-up rollout 대기에 loki/alloy 추가 + Loki `/ready` 스모크.
2. ArgoCD 설치 매니페스트 버전 고정(`v3.4.4`).

## 검증

- `bash -n scripts/k8s-local-up.sh`, `bash -n scripts/argocd-install.sh` 통과.
- `scripts/check-dev-rules.sh`(AI_REPO_DEV_RULES_BASE=origin/main) PASS.
- 스크립트는 활성 클러스터에 실행하지 않음(라이브 클러스터 사용 중).

## 관련 문서

- GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/120
- `docs/adr/0059-k8s-deploy-and-observability.md`
- `docs/development/ci-cd-gitops.md`
