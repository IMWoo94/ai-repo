# k8s 자격증명 Secret 주입(DB/운영 토큰) 전환

## 배경

`deploy/k8s/app.yaml`·`postgres.yaml`이 DB 비밀번호와 운영 토큰 기본값을 평문 env로 둔다. 로컬 학습 전용 고정값이지만 k8s Secret 리소스로 옮기는 것은 저비용이고 원격 전환 대비 올바른 습관이다(#116).

## 목표

- Secret 매니페스트(로컬 고정값)를 신설하고 `secretKeyRef`로 참조 전환한다(비밀 아닌 env는 평문 유지).
- GitOps overlay(`deploy/gitops`)에서 동일하게 렌더된다.
- 가이드 문서의 접속 정보 표에 자격증명 위치(secrets.yaml)를 안내한다.

## 완료 조건

- [x] `Secret ai-repo-credentials`(로컬 고정값) 신설 + `secretKeyRef` 참조로 전환.
- [x] `kubectl kustomize deploy/k8s`·`deploy/gitops` 렌더 성공(라이브 클러스터 미적용).
- [x] 가이드 문서 접속 정보 표·문구를 secrets.yaml 출처 기준으로 갱신.
- [x] ADR-0061, progress 0072 기록.

비고: local/prod-ready overlay 분리는 원격 전환 결정 시점까지 보류(#116 Human Verify) — 이 이슈는 Secret 주입까지만.

## 관련 문서

- `docs/adr/0061-k8s-secret-injection.md`
- `docs/progress/0072-k8s-secret-injection.md`
- `docs/development/k8s-local-monitoring.md`
