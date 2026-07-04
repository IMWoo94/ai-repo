# ADR-0061: k8s 자격증명 Secret 주입 전환

## 상태

Accepted

## 배경

`deploy/k8s/app.yaml`·`postgres.yaml`이 DB 비밀번호와 운영 토큰을 평문 `env` 값으로 두고 있었다(ADR-0059의 로컬 학습 전용 구성). 값 자체는 `compose.yml`·`application.yml` 기본값과 동일한 로컬 고정값이라 유출되어도 실피해는 없지만, Deployment 매니페스트에 자격증명을 직접 박아두는 것은 다음 문제가 있다.

- 자격증명이 여러 매니페스트에 흩어져 값 변경 시 누락·불일치가 생기기 쉽다.
- 원격/스테이징 클러스터로 전환할 때 "평문 env → Secret 참조" 리팩터링을 그 시점에 몰아서 해야 하고, 그 사이 실수로 로컬 값이 스테이징에 새어 나갈 위험이 있다.

k8s Secret 리소스로 옮기는 것은 저비용이고, 원격 전환 대비 올바른 습관이다(#116/#121).

## 결정

| 항목 | 결정 |
| --- | --- |
| Secret | `deploy/k8s/secrets.yaml`에 `Secret ai-repo-credentials`(Opaque, `stringData`) 신설. `deploy/k8s/kustomization.yaml`에 등록 |
| 참조 방식 | `postgres.yaml`·`app.yaml`의 자격증명 env를 `valueFrom.secretKeyRef`로 전환. 비밀이 아닌 env(URL, 프로파일, 스케줄러 플래그 등)는 평문 유지 |
| 키 | `POSTGRES_DB/USER/PASSWORD`(Postgres 초기화), `AI_REPO_POSTGRES_USERNAME/PASSWORD`(앱 DB 접속), `AI_REPO_OPS_ADMIN_TOKEN`/`AI_REPO_OPS_OPERATOR_TOKEN`(운영 API 토큰), `AI_REPO_AUTH_JWT_SECRET`(엔드유저 JWT 서명) — env 이름은 `application.yml` 바인딩과 정확히 일치 |
| 값 관리 | 로컬 고정값은 `secrets.yaml` 한 곳에서만 관리. 값 변경은 이 파일만 수정 |
| overlay | `deploy/gitops`는 `../k8s` base를 그대로 참조하므로 별도 변경 없이 Secret이 함께 렌더된다 |

## 트레이드오프

### 장점

- 자격증명이 `secrets.yaml` 한 곳으로 모여 변경·초기화가 단순하고 매니페스트 간 불일치가 사라진다.
- 원격 전환 시 매니페스트 구조(secretKeyRef)는 그대로 두고 Secret 주입 경로(External Secrets/SealedSecrets 등)만 교체하면 되므로, "평문 env 리팩터링"이라는 위험한 일괄 작업이 사라진다.
- 앱 매니페스트가 실제 비밀 값을 노출하지 않아, 스테이징에 로컬 값이 딸려 나가는 사고를 구조적으로 줄인다.

### 비용

- **로컬 고정값 Secret도 평문 커밋이다.** `stringData`가 그대로 리포에 들어가므로 이 자체는 "비밀 보호"가 아니다. 이 ADR의 목적은 비밀 은닉이 아니라 **(1) 스테이징 사고 방지(평문 env가 매니페스트에 박혀 원격에 새는 경로 제거)와 (2) 원격 전환 준비(주입 지점 분리)**다. 위협 모델상 값 자체는 여전히 공개 리포에 노출된다.
- env 정의가 `secretKeyRef` 블록으로 길어져 매니페스트 라인 수가 늘어난다.

## 대안

- **평문 env 유지**: 저비용이지만 원격 전환 시 일괄 리팩터링과 유출 위험이 남는다. 습관·구조 측면에서 지금 옮기는 게 낫다.
- **`envFrom`로 Secret 전체 주입**: 간결하지만 어떤 env가 어디서 오는지 매니페스트에서 바로 안 보이고, 비밀 아닌 env와 섞이면 추적이 어렵다. 키별 `secretKeyRef`로 명시성을 택했다.
- **local/prod-ready overlay 분리**: 원격 전환 결정 시점까지 보류(#116 Human Verify). 이 ADR은 Secret 주입까지만 다룬다.
- **암호화 Secret(SealedSecrets/SOPS) 즉시 도입**: 로컬 학습 환경에는 과한 운영 비용. 원격 전환 시점에 도입한다.
