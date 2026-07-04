# 0077. ELK 2단계 로깅 스택 (학습용 opt-in)

## 스펙 목표

- 기존 PLG(Prometheus/Loki/Grafana/Alloy)와 **별개의 학습용 opt-in 스택**으로 ELK(Filebeat→Logstash→Elasticsearch→Kibana)를 구성한다. PLG를 대체하지 않고 병존한다.
- Logstash의 grok 필터로 Spring Boot 로그를 구조화 파싱(`level`, `pid`, `appname`, `thread`, `logger`, `log_message`)해 역색인 검색을 학습한다.
- ArgoCD(`deploy/gitops`)에 포함하지 않고 수동 스크립트로만 기동하는 **opt-in** 스택으로 분리한다(항상 켜지지 않음).
- 로컬 학습 전제: `xpack.security.enabled=false`, 단일노드, ephemeral(emptyDir), 리소스 축소.

## 완료 결과

- **매니페스트**(`deploy/k8s/logging`, 자기완결적 kustomize overlay): `namespace.yaml`(ns `logging`) + `elasticsearch.yaml`(Deployment + ClusterIP 9200/9300 + NodePort `elasticsearch-nodeport` 9200→30920) + `logstash.yaml`(Deployment + ClusterIP 5044 + `logstash-pipeline`/`logstash-settings` ConfigMap) + `kibana.yaml`(Deployment + NodePort 5601→30561) + `filebeat.yaml`(ServiceAccount + ClusterRole/Binding + `filebeat-config` ConfigMap + DaemonSet) + `kustomization.yaml`.
- **버전/이미지**: Elastic Stack **8.16.1**(`docker.elastic.co/{elasticsearch,logstash,kibana}/…:8.16.1`, `…/beats/filebeat:8.16.1`).
- **grok 파이프라인**: `input{beats 5044} → filter{grok + date + mutate strip} → output{elasticsearch spring-logs-%{+YYYY.MM.dd}}`. grok 실패 시에도 원본 `message`는 유지되어 ES로 전달되며 `_grokparsefailure_spring` 태그로 식별 가능.
- **스크립트**: `scripts/elk-local-up.sh`(apply -k + ES→Logstash→Kibana rollout + Filebeat DaemonSet 대기 + 접속 안내), `scripts/elk-local-down.sh`(delete -k). `CONTEXT=${CONTEXT:-docker-desktop}`.
- **문서**: `docs/adr/0066-elk-logging-stack.md`, `docs/development/elk-local-logging.md`(구동·접속·KQL 검색·트러블슈팅·LogQL↔KQL 치트시트), `docs/learning/elk-stack.md`(4-tier 아키텍처·컴포넌트 심화·grok 원리·PLG vs ELK 비교), progress 0077, issue-draft 0077.

## 개선 건수

1. 학습용 opt-in ELK 로깅 스택(`deploy/k8s/logging`) 신설 — PLG와 병존, ArgoCD 미포함.
2. Logstash grok 파이프라인으로 Spring 로그 구조화 파싱과 `spring-logs-*` 인덱스 색인.
3. 기동/제거 스크립트와 학습·가이드 문서 신설.

## 검증

- `kubectl --context docker-desktop apply -k deploy/k8s/logging` → ES/Logstash/Kibana rollout Ready + Filebeat DaemonSet Ready
- ES 확인: `http://localhost:30920/_cluster/health`(status green/yellow), `_cat/indices?v`에 `spring-logs-YYYY.MM.dd` 생성
- Kibana `http://localhost:30561` → Discover → data view `spring-logs-*` 생성 → `level: ERROR`, `log_message: *timeout*` KQL 검색
- grok 파싱 필드(`level`, `logger`, `thread`, `log_message`) 노출 및 `_grokparsefailure_spring` 태그 부재 확인

## 남은 일

- GitHub Issue 생성 후 링크 연결(현재 미정).
- 실배포 스모크(Docker Desktop k8s 리소스 여유 필요, 파드 pending=리소스 부족 주의)로 end-to-end 색인·검색 확인.
- 학습 확장 과제: ES Ingest Pipeline(무-Logstash) 대안 토폴로지 비교, ILM 기반 인덱스 수명주기 정책.

## 관련 문서

- GitHub Issue: (미정 — 작성 후 연결)
- `docs/adr/0066-elk-logging-stack.md`
- `docs/development/elk-local-logging.md`
- `docs/learning/elk-stack.md`
- `issue-drafts/0077-elk-logging-stack.md`
