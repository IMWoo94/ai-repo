# 로컬 ELK 로깅 스택 (opt-in) — Filebeat → Logstash → Elasticsearch → Kibana

기존 PLG(Prometheus/Loki/Grafana/Alloy)와 **별개의 학습용 opt-in 로깅 스택**을 Docker Desktop 내장 Kubernetes에 기동하는 가이드. PLG를 대체하지 않고 병존하며, 항상 켜지지 않는다(ArgoCD 미포함). 로그를 grok로 **파싱**해 필드 단위(`level`, `logger`, `log_message` …)로 검색하는 역색인 방식을 학습하는 것이 목적이다.

배경/결정은 `docs/adr/0066-elk-logging-stack.md`, 원리 심화는 `docs/learning/elk-stack.md` 참고.

## 전제 조건

- Docker Desktop 실행 중, **Settings → Kubernetes → Enable Kubernetes** 활성화
- `kubectl` 설치 (`docker-desktop` context 자동 생성됨)
- **메모리 여유**: ELK는 무겁다. Elasticsearch 단독으로 `-Xms1g -Xmx1g`(요청 1536Mi / 제한 2560Mi), Logstash·Kibana 각 768Mi~1280Mi를 쓴다. Docker Desktop **Settings → Resources → Memory를 최소 6~8GB**로 올려야 pod가 pending 없이 뜬다. PLG 스택까지 동시에 띄우려면 더 필요하다.
- ai-repo 앱이 이미 떠 있어야 로그가 수집된다(`./scripts/k8s-local-up.sh`). Filebeat는 `/var/log/containers/*ai-repo*.log`만 tail하므로 앱이 없으면 인덱스가 비어 있다.

## 기동 / 정리

```bash
./scripts/elk-local-up.sh    # kubectl apply -k → ES·Logstash·Kibana 롤아웃 + Filebeat DaemonSet 대기
./scripts/elk-local-down.sh  # logging 네임스페이스 전체 제거 (emptyDir이므로 데이터도 소멸)
```

기동 스크립트는 `logging` 네임스페이스에 매니페스트를 적용하고 ES → Logstash → Kibana 순으로 `rollout status`(각 300s)를 기다린 뒤 Filebeat DaemonSet ready까지 확인하고 접속 URL을 출력한다. `CONTEXT` 환경변수로 context를 바꿀 수 있다(기본 `docker-desktop`).

```bash
CONTEXT=my-cluster ./scripts/elk-local-up.sh   # 다른 context에 기동
```

### 켜기 / 끄기 옵션 (토글)

ELK는 두 가지 방식으로 on/off 한다.

1. **독립 제어** — 위 `elk-local-up.sh` / `elk-local-down.sh`. PLG 스택과 무관하게 ELK만 켜고 끈다.
2. **메인 스택과 함께 (env 플래그)** — `AI_REPO_ELK_ENABLED=true`를 주면 메인 `k8s-local-up.sh`/`k8s-local-down.sh`가 ELK도 함께 기동/정리한다. 기본값은 off라 아무 설정 없이 실행하면 PLG만 뜬다.

```bash
AI_REPO_ELK_ENABLED=true ./scripts/k8s-local-up.sh    # PLG + ELK 함께 기동
AI_REPO_ELK_ENABLED=true ./scripts/k8s-local-down.sh  # PLG + ELK 함께 정리
./scripts/k8s-local-up.sh                             # (기본) PLG만 — ELK off
```

> ELK는 리소스 부담(ES 힙 1g+)이 크므로 **기본 off**로 두고 학습할 때만 켜는 것을 권장한다.

### 로그 수집 범위 (autodiscover)

Filebeat는 `paths` glob 대신 **kubernetes autodiscover**로 라벨 `app=ai-repo` 파드의 컨테이너 로그만 수집한다. (파일명이 `<pod>_<namespace>_<container>.log` 형식이라 `*ai-repo*` glob은 네임스페이스명 `ai-repo`까지 매칭해 alloy/loki 등 타 파드 로그가 섞이므로 사용하지 않는다.) 덕분에 Logstash grok은 Spring Boot 로그만 파싱한다.

## 접속 정보 (URL · 인증)

| 서비스 | URL | 인증 |
| --- | --- | --- |
| Kibana | http://localhost:30561 | **없음** — 로컬 학습 전용 `xpack.security.enabled=false` |
| Kibana Discover | http://localhost:30561/app/discover | 없음 |
| Elasticsearch API | http://localhost:30920 | 없음 (NodePort, API 확인용) |
| 클러스터 헬스 | http://localhost:30920/_cluster/health | 없음 — `status: green\|yellow` 확인 |
| 인덱스 목록 | http://localhost:30920/_cat/indices?v | 없음 — `spring-logs-YYYY.MM.dd` 확인 |

> **보안 주의**: 로컬 학습 편의를 위해 인증을 끈(`xpack.security.enabled=false`) 단일노드·ephemeral(emptyDir) 구성이다. 데이터는 pod 재시작 시 사라지며, 원격/프로덕션에서는 절대 이 설정을 쓰지 않는다.

## Kibana에서 로그 보기 (Discover)

1. http://localhost:30561 접속 → 좌측 메뉴 **☰ → Analytics → Discover**
2. 최초 진입 시 **data view**가 없으면 생성 창이 뜬다. **Create data view** 클릭
   - **Name**: `spring-logs` (임의)
   - **Index pattern**: `spring-logs-*`  ← 날짜별 인덱스를 한 번에 매칭
   - **Timestamp field**: `@timestamp` (Logstash가 로그 시각을 여기에 매핑)
   - **Save data view to Kibana**
3. Discover 상단 검색창에서 KQL/Lucene로 검색. 우상단 시간 범위(기본 Last 15 minutes)를 넉넉히(Last 1 hour) 잡아야 로그가 보인다.

### 파싱된 필드

Logstash grok가 Spring Boot 로그 한 줄을 아래 필드로 분해한다. Discover 좌측 **Available fields**에서 확인·추가한다.

| 필드 | 의미 | 예시 |
| --- | --- | --- |
| `@timestamp` | 로그 발생 시각(ISO8601 → 매핑) | `2026-07-04T09:50:42.320Z` |
| `level` | 로그 레벨 | `INFO`, `ERROR`, `WARN` |
| `pid` | 프로세스 ID | `1` |
| `appname` | 앱 이름(`[...]` 안) | `ai-repo` |
| `thread` | 스레드명 | `main`, `http-nio-8080-exec-1` |
| `logger` | 로거(클래스) | `com.imwoo.airepo.AiRepoApplication` |
| `log_message` | 실제 메시지 | `Started AiRepoApplication in 15.044 seconds` |
| `message` | 원본 전체 줄(파싱 실패해도 유지) | (한 줄 전체) |

> grok가 실패해도 **원본 `message`는 그대로 ES에 저장**되고 `_grokparsefailure_spring` 태그가 붙는다(트러블슈팅 참고). 로그가 유실되지 않는다.

## 검색 예시 (KQL / Lucene)

Discover 검색창 기본은 **KQL**. Lucene으로 바꾸려면 검색창 우측 언어 토글을 쓴다.

```text
# ── KQL ──────────────────────────────────────────────
level: ERROR                         # 에러 레벨만
level: ERROR or level: WARN          # 에러 + 경고
log_message: *timeout*               # 메시지에 timeout 포함 (와일드카드)
logger: *OutboxRelay*                # 특정 로거(클래스)
thread: main and level: INFO         # 조건 결합
not level: INFO                      # INFO 제외
log_message: "wallet-001"            # 특정 지갑 추적(따옴표 = 구문)
tags: _grokparsefailure_spring       # 파싱 실패한 로그만 (grok 디버깅)

# ── Lucene ───────────────────────────────────────────
level:ERROR AND log_message:*timeout*
log_message:/.*[Tt]ransfer.*/        # 정규식
```

## 파이프라인 확인 (kubectl)

로그가 Kibana에 안 보이면 **Filebeat 수집 → Logstash 파싱 → ES 색인** 순서로 각 단계를 점검한다.

```bash
# ── 파드 상태 (모두 Running / DaemonSet DESIRED=CURRENT 확인) ──────
kubectl --context docker-desktop -n logging get pods -o wide
kubectl --context docker-desktop -n logging get ds filebeat

# ── Filebeat: ai-repo 로그를 잡아 Logstash로 보내는지 ─────────────
kubectl --context docker-desktop -n logging logs ds/filebeat --tail=100 -f
#   "Harvester started for file: /var/log/containers/...ai-repo..." 가 보이면 수집 중
#   "Connecting to backoff(async(tcp://logstash:5044))" 연결 확인

# ── Logstash: grok 파싱 / ES 출력 상태 ───────────────────────────
kubectl --context docker-desktop -n logging logs deploy/logstash --tail=100 -f
#   "Pipeline started" / beats input 5044 리슨 / elasticsearch output 연결 확인

# ── Elasticsearch: 인덱스가 생성되고 문서가 쌓이는지 ──────────────
kubectl --context docker-desktop -n logging logs deploy/elasticsearch --tail=50
curl -s http://localhost:30920/_cat/indices?v                    # spring-logs-YYYY.MM.dd 행 + docs.count
curl -s http://localhost:30920/_cluster/health?pretty            # status green/yellow
curl -s 'http://localhost:30920/spring-logs-*/_search?size=1&pretty'  # 문서 1건 직접 조회(파싱 필드 확인)

# ── 파드 이벤트 (pending 원인 / OOM / probe 실패) ─────────────────
kubectl --context docker-desktop -n logging get events --sort-by=.lastTimestamp
```

## 리소스 주의

- ELK는 로컬에서 가장 무거운 스택이다. **Elasticsearch만으로 힙 1GB + 오버헤드**를 쓰고, 세 컴포넌트 합쳐 요청 3GB / 제한 5GB 수준이다. Docker Desktop 메모리가 부족하면 pod가 `Pending`에 머문다.
- 학습이 끝나면 **반드시 `./scripts/elk-local-down.sh`로 내려서** 메모리를 회수한다. emptyDir이라 어차피 데이터는 보존되지 않으니 상시 켜둘 이유가 없다.
- PLG와 ELK를 **동시에** 띄우면 메모리 압박이 커진다. 학습 시에는 한 번에 하나만 권장.

## 트러블슈팅

| 증상 | 원인 | 조치 |
| --- | --- | --- |
| pod가 `Pending`에서 안 뜸 | 노드 메모리·CPU 부족(스케줄 불가) | `kubectl -n logging describe pod <name>`의 Events에서 `Insufficient memory` 확인 → Docker Desktop 메모리 상향(6~8GB↑) 또는 PLG 스택 내리기 |
| Kibana `Kibana server is not ready yet` | ES가 아직 green/yellow 아님(초기화 중) | 30~60초 대기. `curl :30920/_cluster/health`로 status 확인. ES pod가 OOM이면 events 확인 |
| Discover에 로그가 없음 | ① 시간 범위 좁음 ② 앱 미기동 ③ 인덱스 미생성 | 시간 범위를 Last 1 hour로 확대 · ai-repo 앱 기동 확인 · `_cat/indices`에 `spring-logs-*` 있는지 확인 |
| 필드가 파싱 안 되고 `message`만 있음 | grok 매칭 실패 | KQL `tags: _grokparsefailure_spring`로 실패 로그 확인. 로그 포맷이 스펙(`ISO8601 LEVEL PID --- [app] [thread] logger : msg`)과 다르면 `deploy/k8s/logging/logstash.yaml`의 grok 패턴을 조정 |
| data view 생성 시 인덱스가 안 보임 | 아직 문서가 색인되지 않음 | 로그가 최소 1건 ES에 들어와야 패턴이 매칭된다. 파이프라인 확인 절차로 Filebeat→Logstash→ES 흐름 점검 |
| Filebeat 로그에 harvester 없음 | ai-repo 파드가 없거나 다른 노드 | 앱 기동 확인. Filebeat는 DaemonSet이라 노드마다 뜨며 `/var/log/containers/*ai-repo*.log`만 tail |

> **grok 실패해도 로그는 유실되지 않는다.** 파싱에 실패한 줄은 `_grokparsefailure_spring` 태그와 함께 원본 `message`를 담아 ES로 그대로 전달된다. 즉 파싱 실패 = 검색 필드 부재이지 로그 손실이 아니다.

## PLG(Loki) LogQL ↔ ELK KQL 치트시트

같은 로그를 두 스택에서 어떻게 검색하는지 대응표. **핵심 차이**: Loki는 라벨(`app`, `namespace`)로 스트림을 고른 뒤 본문을 grep(`|=`, `|~`)한다(라벨 인덱스). ELK는 grok로 미리 쪼갠 **필드**를 역색인으로 직접 질의한다(`level: ERROR`처럼 필드=값).

| 목적 | Loki LogQL (Grafana Explore) | ELK KQL (Kibana Discover) |
| --- | --- | --- |
| 앱 전체 로그 | `{app="ai-repo"}` | (data view `spring-logs-*` 전체, 쿼리 비움) |
| 에러만 | `{app="ai-repo"} |= "ERROR"` | `level: ERROR` |
| 에러 + 경고 | `{app="ai-repo"} |~ "ERROR|WARN"` | `level: ERROR or level: WARN` |
| 부분 문자열(timeout) | `{app="ai-repo"} |= "timeout"` | `log_message: *timeout*` |
| 정규식 매칭 | `{app="ai-repo"} |~ "Transfer|TRANSFER"` | `log_message: *ransfer*` (또는 Lucene `log_message:/.*[Tt]ransfer.*/`) |
| 특정 로거/클래스 | `{app="ai-repo"} |= "OutboxRelay"` | `logger: *OutboxRelay*` |
| 특정 값 추적 | `{app="ai-repo"} |= "wallet-001"` | `log_message: "wallet-001"` |
| 제외 | `{app="ai-repo"} != "INFO"` | `not level: INFO` |
| 조건 결합 | `{app="ai-repo"} |= "ERROR" |= "timeout"` | `level: ERROR and log_message: *timeout*` |
| 발생률/집계 | `sum by (app) (rate({namespace="ai-repo"}[5m]))` | Kibana **Lens/Visualize**로 `@timestamp` 히스토그램 집계 |

> 한 줄 요약: **Loki = 라벨로 좁히고 본문 grep(쓰기 저렴/검색은 스캔), ELK = 필드 역색인 직접 질의(쓰기 비쌈/필드 검색 강력).** 자세한 비교는 `docs/learning/elk-stack.md`.

## 이슈 트래킹

로그에서 문제(에러 급증, 파싱 실패, 파드 크래시)를 발견하면:

1. Kibana Discover의 KQL 쿼리 결과 스크린샷 또는 `curl :30920/spring-logs-*/_search` 출력, `kubectl describe` 출력을 증적으로 수집
2. `issue-drafts/`에 `.github/ISSUE_TEMPLATE/bug.yml` 형식으로 초안 작성(재현 절차 + 증적 포함)
3. GitHub Issue 생성 후 `issue-drafts/README.md` 목록에 링크 기록, 수정 PR에 이슈 연결

## 관련 문서

- `docs/adr/0066-elk-logging-stack.md` — ELK 도입 결정(PLG 병존, opt-in, 2단계 Logstash grok 선택 이유)
- `docs/learning/elk-stack.md` — ELK 4-tier 아키텍처·grok 원리·PLG vs ELK 심화
- `docs/development/k8s-local-monitoring.md` — 기본 PLG(Prometheus/Grafana/Loki) 로컬 모니터링
- `deploy/k8s/logging/README.md` — 매니페스트 overlay 구성 설명
