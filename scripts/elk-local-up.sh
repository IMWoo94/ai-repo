#!/usr/bin/env bash
# 로컬 Docker Desktop k8s에 opt-in ELK 로깅 스택(Filebeat -> Logstash -> Elasticsearch -> Kibana)을 배포한다.
# PLG(Prometheus/Loki/Grafana)와 별개의 학습용 스택이며, ArgoCD에는 포함되지 않는다(수동 기동).
set -euo pipefail
cd "$(dirname "$0")/.."

CONTEXT=${CONTEXT:-docker-desktop}

echo "==> K8s 확인"
kubectl --context "$CONTEXT" get nodes >/dev/null || {
  echo "docker-desktop k8s 클러스터에 연결할 수 없습니다 (Docker Desktop 설정에서 Kubernetes 활성화)."; exit 1; }

echo "==> 매니페스트 적용 (namespace logging)"
kubectl --context "$CONTEXT" apply -k deploy/k8s/logging

echo "==> 롤아웃 대기 (ES -> Logstash -> Kibana)"
kubectl --context "$CONTEXT" -n logging rollout status deployment/elasticsearch --timeout=300s
kubectl --context "$CONTEXT" -n logging rollout status deployment/logstash --timeout=300s
kubectl --context "$CONTEXT" -n logging rollout status deployment/kibana --timeout=300s

echo "==> Filebeat DaemonSet 대기"
kubectl --context "$CONTEXT" -n logging rollout status daemonset/filebeat --timeout=300s

cat <<'EOF'

배포 완료 (opt-in ELK 로깅 스택):
  Kibana         http://localhost:30561          (Discover -> data view 'spring-logs-*' 생성)
  Elasticsearch  http://localhost:30920          (health: /_cluster/health)
  인덱스 패턴     spring-logs-*                   (일자별 spring-logs-YYYY.MM.dd)

로그 흐름: Filebeat(ai-repo 컨테이너 로그) -> Logstash:5044(grok 파싱) -> Elasticsearch:9200 -> Kibana:5601
정리: scripts/elk-local-down.sh
EOF
