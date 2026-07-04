# ADR-0066: 학습용 opt-in ELK 로깅 스택

## 상태

Accepted

## 배경

ai-repo의 기본 관측 스택은 PLG(Prometheus/Loki/Grafana/Alloy)이며, 로그는 Alloy가 수집해 Loki에 적재한다(ADR-0059). Loki는 로그 본문을 인덱싱하지 않고 라벨(label)만 인덱싱한 뒤 본문은 압축 저장하고 조회 시 grep 방식으로 훑는 구조라 운영 비용이 낮다.

한편 로그 파이프라인의 대표 구성인 ELK(Elasticsearch/Logstash/Kibana + Filebeat)는 로그 본문을 역색인(inverted index)으로 색인해 임의 필드/전문 검색을 빠르게 지원하고, Logstash grok으로 비정형 로그를 구조화하는 파싱 단계를 학습할 수 있다. 이 저장소는 학습 목적을 겸하므로, PLG를 대체하지 않고 ELK를 **직접 만져보며 두 접근(라벨-grep vs 역색인, 파싱 유무)의 차이를 체감**할 수단이 필요했다.

문제는 ELK가 상시 켜두기에는 무겁다는 점이다. Elasticsearch 단독으로도 JVM 힙 1g 이상을 요구하고 Logstash/Kibana까지 더하면 로컬 리소스를 크게 점유한다. 상시 배포(ArgoCD) 대상에 넣으면 학습 의도와 무관하게 항상 자원을 소모한다.

## 결정

Filebeat → Logstash(grok) → Elasticsearch → Kibana 4-tier ELK 스택을 **학습용 opt-in 모듈**로 도입하고, 기존 PLG와 **병존(대체 아님)**시킨다.

| 항목 | 결정 |
| --- | --- |
| 토폴로지 | Filebeat(수집) → Logstash(파싱/변환) → Elasticsearch(색인/저장) → Kibana(검색/시각화)의 **2단계(Logstash 경유)** 구성. Beats가 ES로 직접 보내는 1단계 대신 Logstash를 끼워 grok 파싱을 학습 |
| 배치 위치 | 앱(`ai-repo`)과 분리된 `logging` 네임스페이스에 자기완결적 kustomize overlay(`deploy/k8s/logging/`)로 구성 |
| opt-in | ArgoCD GitOps(`deploy/gitops`)에 **포함하지 않는다**. 항상 켜지지 않으며 `scripts/elk-local-up.sh`/`elk-local-down.sh`로 수동 기동·제거 |
| 파싱 | Logstash pipeline에서 Spring Boot 로그(`ISO8601 LEVEL PID --- [app] [thread] logger : message`)를 grok으로 분해해 `level`/`pid`/`thread`/`logger`/`log_message` 필드화. grok 실패 시 `_grokparsefailure_spring` 태그를 붙이되 원본 `message`는 유지해 ES로 전달 |
| 인덱스 | `spring-logs-%{+YYYY.MM.dd}` 일자별 인덱스, Kibana data view `spring-logs-*` |
| 버전 | Elastic Stack **8.16.1** 고정(ES/Logstash/Kibana/Filebeat 동일 버전) |
| 로컬 전제 | `xpack.security.enabled=false`(인증 없음), `discovery.type=single-node`, 데이터는 emptyDir(ephemeral), 힙/리소스 축소 |
| 접속 | Kibana NodePort 30561(`http://localhost:30561`), Elasticsearch NodePort 30920(`http://localhost:30920`) |

## 왜 PLG를 두고 ELK를 별도로 두는가

PLG와 ELK는 로그를 다루는 철학이 다르다. Loki는 라벨만 인덱싱하고 본문은 grep으로 훑어 저장·운영 비용이 낮은 대신 임의 필드 검색이 느리다. Elasticsearch는 본문을 역색인해 임의 필드·전문 검색이 빠른 대신 색인 비용과 리소스가 크다. **어느 쪽이 우월한 것이 아니라 트레이드오프가 반대 방향**이므로, 기본 운영 스택(PLG)은 그대로 두고 ELK는 두 접근을 비교 학습하는 대안으로 병존시킨다.

또한 Logstash grok 파싱 단계 자체가 학습 대상이다. Loki 경로에는 없는 "비정형 로그 → 구조화 필드" 변환을 직접 작성해봄으로써 파이프라인의 파싱 계층을 이해할 수 있다.

## 왜 상시 배포가 아니라 opt-in인가

ELK는 상시 켜두기에 무겁다(ES JVM 힙 1g + Logstash/Kibana). 학습 목적의 스택을 ArgoCD 상시 배포에 넣으면 필요 없을 때도 로컬 자원을 계속 점유한다. 그래서 GitOps 대상에서 제외하고, 필요할 때만 스크립트로 올렸다가 내리는 opt-in으로 둔다. `logging` 네임스페이스 분리와 자기완결적 overlay 덕분에 앱/PLG 스택에 영향 없이 기동·제거가 가능하다.

## 로컬 전제와 라이선스

- **비프로덕션 전제**: `xpack.security.enabled=false`로 인증·TLS를 끈다. 학습 편의를 위한 것이며 클러스터 외부에 노출하지 않는 로컬 사용을 전제로 한다. 프로덕션에서는 보안 활성화와 자격증명 주입이 필수다.
- **단일 노드·ephemeral**: `discovery.type=single-node`, 데이터는 emptyDir이라 파드 재기동 시 인덱스가 사라진다. 영속성은 학습 범위 밖으로 둔다(PVC는 후속 과제).
- **리소스**: ES 힙 `-Xms1g -Xmx1g`, Logstash `-Xmx512m`로 축소하고 각 컴포넌트에 requests/limits를 건다. 그래도 로컬에서 상당한 메모리를 요구하므로 파드 pending 시 리소스 부족을 우선 의심한다.
- **라이선스**: Elastic Stack 8.16은 **AGPLv3 옵션**이 존재한다(SSPL/Elastic License 외 선택지). 성숙도가 높고 자료가 풍부한 8.16.1을 학습 버전으로 고정한다.

## 트레이드오프

### 장점

- PLG(라벨-grep)와 ELK(역색인)를 같은 저장소에서 나란히 비교·학습할 수 있다.
- Logstash grok으로 파싱 계층을 직접 다뤄본다. grok 실패해도 원본 로그가 보존돼 데이터 유실 없이 패턴을 반복 교정할 수 있다.
- `logging` 네임스페이스 + opt-in 분리로 기본 운영 스택(앱/PLG)과 완전히 격리된다.

### 비용

- ELK는 무겁다. 상시 배포가 아닌 수동 기동이라 항상 최신 로그를 보지 못하고, 올릴 때마다 리소스를 확보해야 한다.
- 보안 off·단일노드·ephemeral 전제라 프로덕션 구성과 거리가 있다. 학습용 구성을 그대로 운영에 쓸 수 없다.
- 스택이 두 벌(PLG·ELK) 존재하므로 문서/구성의 인지 부담이 늘어난다. opt-in·별도 네임스페이스로 격리해 완화한다.

## 대안

- **PLG로 통일(ELK 미도입)**: 가장 단순하고 운영 스택이 하나로 유지되지만, 역색인·grok 파싱을 학습할 수단이 없어 이 결정의 학습 목표를 충족하지 못한다.
- **ELK 1단계(Filebeat → ES 직접, Logstash 생략)**: 구성이 가볍지만 grok 파싱 계층 학습이 빠진다. ES Ingest Pipeline으로 파싱을 대체할 수 있으나, 파이프라인 학습 관점에서 독립 Logstash를 두는 편이 명확하다.
- **ELK 상시 배포(ArgoCD 포함)**: 항상 최신 로그를 검색할 수 있으나 학습용 스택이 상시 리소스를 점유해 로컬 부담이 크다. opt-in 의도와 맞지 않는다. 대신 `AI_REPO_ELK_ENABLED` env 플래그·독립 스크립트로 제어하거나, `automated`(selfHeal)를 두지 않은 **별도 ArgoCD Application**(`deploy/argocd/logging-application.yaml`)으로 UI/CLI에서 수동 Sync/Delete 토글한다. 기존 `ai-repo` App(automated)에 편입하면 항상 켜져 opt-in이 깨지므로 분리한다.
- **보안 활성화 구성**: 프로덕션에 가깝지만 인증서·자격증명 설정 비용이 커 로컬 학습 진입장벽을 높인다. 범위 밖으로 둔다.
