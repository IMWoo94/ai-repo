# k8s 로컬 스크립트 하드닝 (loki/alloy 대기 + ArgoCD 버전 고정)

## 배경

- `scripts/k8s-local-up.sh` rollout 대기 대상이 postgres/app/prometheus/grafana뿐 — kustomization에 포함된 loki/alloy는 대기·확인 없음.
- `scripts/argocd-install.sh`가 원격 `stable` 매니페스트를 직접 apply — 태그가 이동해 재현성이 없음.

## 목표

- 로컬 up 스크립트가 관측 스택(loki/alloy)까지 기동을 대기·확인해 부분 기동 상태를 감춘 채 "완료"를 출력하지 않게 한다.
- ArgoCD 설치를 고정 버전으로 재현 가능하게 만든다.

## 완료 조건

- [x] `loki`/`alloy` rollout status 대기 추가 + Loki `/ready` 최소 스모크(port-forward + curl, 정리 포함).
- [x] ArgoCD 설치 매니페스트 버전 고정(`v3.4.4` URL) 및 가이드 문서(`docs/development/ci-cd-gitops.md`) 갱신.
- [x] ADR-0059 결정 표에 rollout 대기 범위·ArgoCD 버전 고정 반영, progress 0071 기록.

## 관련 문서

- GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/120
- `docs/progress/0071-k8s-script-hardening.md`
- `docs/adr/0059-k8s-deploy-and-observability.md`
