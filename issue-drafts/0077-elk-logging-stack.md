# ELK 2단계 로깅 스택 (학습용 opt-in)

## 배경

ai-repo는 기본 관측 스택으로 PLG(Prometheus/Loki/Grafana/Alloy)를 사용한다(0069). Loki는 라벨 인덱스 + grep 방식이라 가볍지만, 역색인 기반 전문 검색과 필드 파싱(grok) 같은 로그 파이프라인 학습에는 적합하지 않다. 학습 목적으로 Filebeat→Logstash→Elasticsearch→Kibana(ELK) 스택을 **PLG와 병존하는 별개의 opt-in 모듈**로 도입한다. PLG를 대체하지 않으며, 항상 켜두지 않고 필요 시 수동 기동한다.

## 제안

- `deploy/k8s/logging`에 자기완결적 kustomize overlay로 ELK 4-tier(Filebeat DaemonSet → Logstash grok → Elasticsearch → Kibana)를 구성한다.
- Logstash grok 필터로 Spring Boot 로그(`ISO8601 LEVEL PID --- [appname] [thread] logger : message`)를 `level`/`pid`/`appname`/`thread`/`logger`/`log_message` 필드로 파싱하고 `spring-logs-%{+YYYY.MM.dd}` 인덱스에 색인한다.
- Elastic Stack **8.16.1**, 네임스페이스 `logging`(앱과 분리), 서비스/포트 `elasticsearch:9200`(NodePort 30920) · `logstash:5044` · `kibana:5601`(NodePort 30561).
- 로컬 학습 전제로 `xpack.security.enabled=false`, 단일노드, ephemeral(emptyDir), 리소스 축소.
- **ArgoCD(`deploy/gitops`)에 포함하지 않고** `scripts/elk-local-up.sh`/`elk-local-down.sh`로만 기동·제거하는 opt-in으로 유지한다.
- ADR-0066, 가이드(`docs/development/elk-local-logging.md`), 학습문서(`docs/learning/elk-stack.md`)를 남긴다.

## 완료 조건

- [ ] `deploy/k8s/logging` kustomize 스택과 `scripts/elk-local-up.sh`/`elk-local-down.sh`로 기동·제거된다.
- [ ] Filebeat가 라벨 `app=ai-repo` autodiscover로 앱 컨테이너 로그만 수집해 Logstash로 전달하고, grok으로 파싱된 필드가 ES에 색인된다.
- [ ] `http://localhost:30920/_cat/indices`에 `spring-logs-YYYY.MM.dd`가 생성된다.
- [ ] Kibana(`http://localhost:30561`) Discover에서 data view `spring-logs-*`로 `level: ERROR` 등 KQL 검색이 된다.
- [ ] grok 실패 시에도 원본 `message`가 보존되고 `_grokparsefailure_spring` 태그로 식별된다.
- [ ] 이 스택이 ArgoCD에 포함되지 않는(opt-in) 상태가 유지된다.
- [ ] `AI_REPO_ELK_ENABLED=true`로 메인 `k8s-local-up.sh`/`down.sh`와 함께 켜고 끌 수 있다.
- [ ] ADR-0066, progress 0077, 가이드/학습 문서가 기록된다.

## 검증

```bash
kubectl --context docker-desktop apply -k deploy/k8s/logging
scripts/elk-local-up.sh
# ES/Kibana 확인 후
scripts/elk-local-down.sh
```

## 관련 문서

- `docs/adr/0066-elk-logging-stack.md`
- `docs/progress/0077-elk-logging-stack.md`
- `docs/development/elk-local-logging.md`
- `docs/learning/elk-stack.md`

관련: progress 0069(PLG 관측 스택), ADR-0066.

GitHub Issue: (미정 — 작성 후 연결)
