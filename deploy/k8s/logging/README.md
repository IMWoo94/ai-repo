# ELK 로깅 스택 (opt-in, 학습용)

Filebeat → Logstash(grok) → Elasticsearch → Kibana 로그 파이프라인의 자기완결적 kustomize overlay다.
기존 PLG(Prometheus/Loki/Grafana/Alloy)와 **별개의 학습용 스택**으로, PLG를 대체하지 않고 병존한다.

> **opt-in**: 이 overlay는 ArgoCD GitOps(`deploy/gitops`)에 **포함되지 않는다**. 항상 켜지지 않으며,
> 아래 스크립트로만 수동 기동/제거한다. 로컬 학습 전제(`xpack.security.enabled=false`, 단일노드,
> ephemeral `emptyDir`, 리소스 축소)이므로 프로덕션 용도가 아니다.

## 구성요소

- **namespace.yaml** — `logging` 네임스페이스 (앱 `ai-repo`와 분리).
- **elasticsearch.yaml** — Elasticsearch 8.16.1 단일노드. ClusterIP(9200/9300) + NodePort(30920, API 확인용). 역색인 저장/검색 엔진.
- **logstash.yaml** — Logstash 8.16.1. beats(5044) 입력을 grok으로 파싱해 ES로 전송. 파이프라인/설정 ConfigMap 포함.
- **kibana.yaml** — Kibana 8.16.1. NodePort(30561)로 노출되는 검색/시각화 UI.
- **filebeat.yaml** — Filebeat 8.16.1 DaemonSet. `*ai-repo*` 컨테이너 로그를 수집해 Logstash로 전송(ServiceAccount/RBAC 포함).
- **kustomization.yaml** — 위 리소스를 묶는 overlay.

## 기동 / 제거

```bash
scripts/elk-local-up.sh      # 배포 + 롤아웃 대기 + 접속 안내
scripts/elk-local-down.sh    # 전체 삭제 (ES 데이터 포함 초기화)
```

`CONTEXT` 환경변수로 kube context를 바꿀 수 있다(기본 `docker-desktop`).

## 접속

| 대상 | URL | 비고 |
| --- | --- | --- |
| Kibana | http://localhost:30561 | Discover → data view `spring-logs-*` 생성 |
| Elasticsearch | http://localhost:30920 | health: `/_cluster/health` |

로그 흐름: **Filebeat → Logstash:5044(grok) → Elasticsearch:9200 → Kibana:5601**, 인덱스 `spring-logs-YYYY.MM.dd`.
grok 파싱에 실패해도 원본 `message`는 유지되어 ES로 전달되므로(`_grokparsefailure_spring` 태그가 붙음) 로그가 유실되지 않는다.
