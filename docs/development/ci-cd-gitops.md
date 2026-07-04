# CI/CD — GitHub Actions → GHCR → GitOps → ArgoCD

main 브랜치 push가 이미지 빌드부터 로컬 k8s 배포까지 자동으로 이어지는 파이프라인 가이드.

## 파이프라인 흐름

```
main push
  → GitHub Actions (.github/workflows/deploy.yml)
      1. build.gradle version + 커밋 sha 앞 8자리 → 태그 {version}-{sha8} (예: 0.7.0-a1b2c3d4)
      2. Docker 이미지 빌드 → ghcr.io/imwoo94/ai-repo:{tag} push
      3. deploy/gitops/kustomization.yaml의 newTag 갱신 → "[skip ci]" 커밋 push
  → ArgoCD (로컬 docker-desktop 클러스터)
      deploy/gitops 변화 감지 → ai-repo 네임스페이스에 자동 sync (prune + selfHeal)
```

- **이미지 태그 규칙**: 반드시 `{version}-{sha8}`. `latest` 사용 금지.
- **재트리거 방지**: deploy.yml은 `deploy/gitops/kustomization.yaml`을 paths-ignore하고, 매니페스트 커밋 메시지에 `[skip ci]`를 붙여 CI/deploy 루프를 차단한다.
- CI 검증(ci.yml: gradle check/scenario/frontend)과 deploy는 독립 실행이다. 학습 랩 단순화를 위한 선택으로, 배포 전 CI 게이트가 필요해지면 `workflow_run` 트리거로 전환한다.

## 디렉토리

```
.github/workflows/deploy.yml   # 빌드→push→매니페스트 갱신
deploy/k8s/                    # base 매니페스트 (로컬 수동 모드도 이 디렉토리 사용)
deploy/gitops/                 # ArgoCD가 sync하는 오버레이 (ghcr 이미지 + newTag)
deploy/argocd/application.yaml # ArgoCD Application 정의
scripts/argocd-install.sh      # 로컬 ArgoCD 설치 + Application 등록
```

## ArgoCD 설치 / 접속

```bash
./scripts/argocd-install.sh    # --server-side apply (CRD 크기 제한 때문에 필수)

# UI
kubectl -n argocd port-forward svc/argocd-server 8443:443
# https://localhost:8443 — admin / 아래 명령의 출력
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d
```

## 배포 상태 확인

```bash
kubectl -n argocd get application ai-repo                     # Synced / Healthy 확인
kubectl -n argocd describe application ai-repo | tail -30     # sync 이력·오류
kubectl -n ai-repo get pods -o wide                           # 실제 파드 이미지 태그 확인
kubectl -n ai-repo get deploy ai-repo -o jsonpath='{.spec.template.spec.containers[0].image}'
```

GitHub Actions 실행 확인: `gh run list --workflow deploy.yml`, 실패 로그는 `gh run view <id> --log-failed`.

## 로컬 수동 모드 vs GitOps 모드

같은 `ai-repo` 네임스페이스를 두 방식이 관리하므로 **동시에 쓰지 않는다**.

| 모드 | 사용 | 전환 |
| --- | --- | --- |
| 수동 (기본) | `./scripts/k8s-local-up.sh` — 로컬 빌드 이미지 즉시 반영, 개발 루프용 | ArgoCD Application 삭제: `kubectl -n argocd delete application ai-repo` |
| GitOps | main 머지 → 자동 배포. selfHeal이 수동 kubectl 변경을 되돌림 | `kubectl apply -f deploy/argocd/application.yaml` |

## 트러블슈팅 / 로그 검색

- **Actions 빌드 실패**: `gh run view --log-failed`. Dockerfile 빌드는 로컬에서 `docker build -t test .`로 재현.
- **ArgoCD sync 실패**: `kubectl -n argocd get app ai-repo -o jsonpath='{.status.conditions}'`. repo-server 로그: `kubectl -n argocd logs deploy/argocd-repo-server --tail=100`.
- **ImagePullBackOff**: ghcr 패키지가 private이면 로컬 노드가 pull 불가 → GitHub Packages에서 public 전환 또는 pull secret 등록. `kubectl -n ai-repo describe pod <pod>`로 원인 확인.
- **앱 런타임 로그**: Grafana Explore(Loki)에서 `{app="ai-repo"} |= "ERROR"` — 자세한 LogQL 가이드는 `docs/development/k8s-local-monitoring.md` 로그 검색 섹션 참고. 즉석 확인은 `kubectl -n ai-repo logs deploy/ai-repo --tail=100 -f`.

## 이슈 트래킹

파이프라인/배포 문제를 발견하면:

1. `issue-drafts/`에 `.github/ISSUE_TEMPLATE/bug.yml`(결함) 또는 `feature.yml`(개선) 형식으로 초안 작성
2. GitHub Issue 생성 후 초안에 이슈 링크 기록 (`issue-drafts/README.md` 목록 갱신)
3. 수정 PR에는 이슈 번호를 연결하고, 배포 관련 결정 변경은 ADR로 남긴다

## 관련 문서

- `docs/development/k8s-local-monitoring.md` — 로컬 k8s 스택, Grafana/Prometheus/Loki
- `docs/adr/0059-k8s-deploy-and-observability.md` — 배포·관측 설계 결정
