# 아키텍처 다이어그램

인프라(배포·관측)와 내부 애플리케이션 구조를 mermaid로 정리한다. GitHub에서 자동 렌더링된다.

## 1. 인프라 아키텍처

GitOps 파이프라인(main push → GHCR → ArgoCD)과 로컬 Docker Desktop k8s 스택. 점선은 GitOps 대신 쓰는 수동 모드(`scripts/k8s-local-up.sh`)와 이미지 pull 경로.

```mermaid
graph TB
    dev["개발자"]

    subgraph GH["GitHub (IMWoo94/ai-repo)"]
        repo["main 브랜치"]
        actions["GitHub Actions<br/>deploy.yml"]
        gitops["deploy/gitops<br/>kustomization.yaml (newTag)"]
    end

    ghcr["GHCR 이미지 레지스트리<br/>ghcr.io/imwoo94/ai-repo<br/>태그: version-sha8"]

    subgraph K8S["Docker Desktop Kubernetes (로컬)"]
        subgraph ARGONS["argocd 네임스페이스"]
            argocd["ArgoCD<br/>자동 sync (prune + selfHeal)"]
        end
        subgraph NS["ai-repo 네임스페이스"]
            app["ai-repo 앱 (Spring Boot)<br/>:8080 / NodePort 30080"]
            pg[("PostgreSQL 17<br/>PVC 1Gi")]
            prom["Prometheus<br/>NodePort 30990"]
            loki["Loki (72h 보존)<br/>PVC 2Gi"]
            alloy["Alloy 로그 수집기"]
            graf["Grafana<br/>NodePort 30300"]
        end
    end

    dev -->|git push main| repo
    repo -->|트리거| actions
    actions -->|이미지 빌드 + push| ghcr
    actions -->|newTag 갱신 커밋, skip ci| gitops
    argocd -->|변경 감지, pull| gitops
    argocd -->|kustomize sync| app
    ghcr -.->|이미지 pull| app
    dev -.->|수동 모드: scripts/k8s-local-up.sh| app

    app -->|JDBC :5432| pg
    prom -->|15s 스크랩 /actuator/prometheus| app
    alloy -->|파드 로그 tail, k8s API| app
    alloy -->|로그 push| loki
    graf -->|PromQL| prom
    graf -->|LogQL| loki
    dev -->|브라우저 localhost:30080 / 30300 / 30990| graf

    classDef actor fill:#fff3cd,stroke:#b45309,stroke-width:2px
    classDef github fill:#dbeafe,stroke:#1d4ed8
    classDef registry fill:#ede9fe,stroke:#7c3aed
    classDef gitopsTool fill:#ffedd5,stroke:#ea580c
    classDef workload fill:#dcfce7,stroke:#16a34a
    classDef observability fill:#fce7f3,stroke:#be185d
    classDef storage fill:#e2e8f0,stroke:#475569

    class dev actor
    class repo,actions,gitops github
    class ghcr registry
    class argocd gitopsTool
    class app workload
    class prom,loki,alloy,graf observability
    class pg storage
    style GH fill:#eff6ff,stroke:#93c5fd
    style K8S fill:#f8fafc,stroke:#94a3b8
    style ARGONS fill:#fff7ed,stroke:#fdba74
    style NS fill:#f0fdf4,stroke:#86efac
```

> 색상: 🟡 개발자 · 🔵 GitHub · 🟣 레지스트리 · 🟠 ArgoCD · 🟢 앱 워크로드 · 🩷 관측(모니터링·로그) · ⚪ 저장소

- 접속 URL·계정: `docs/development/k8s-local-monitoring.md` 접속 정보 표
- 파이프라인 상세: `docs/development/ci-cd-gitops.md`
- 수동 모드와 GitOps 모드는 같은 네임스페이스를 관리하므로 택1

## 2. 애플리케이션 아키텍처 (헥사고날)

`api → application → domain`, 포트 인터페이스를 `infra` 어댑터가 구현한다. 저장소 어댑터는 Spring 프로파일로 교체된다(`!postgres` → InMemory, `postgres` → Jdbc+Flyway).

```mermaid
graph TB
    user["사용자<br/>(React 프론트)"]
    ops["운영자<br/>(X-Operator-Token / X-Admin-Token)"]

    subgraph API["api 계층 — 인바운드 어댑터"]
        sec["SecurityConfig 체인<br/>AdminHeaderAuthenticationFilter<br/>AdminApiAccessAuditFilter"]
        wapi["WalletCommand / Query /<br/>LedgerController"]
        oapi["운영 컨트롤러<br/>outbox review·requeue, relay run,<br/>consumer, alert, pruning, audit"]
    end

    subgraph APPL["application 계층 — 유스케이스 + 포트"]
        cmd["WalletCommandService<br/>충전·송금 멱등 처리 →<br/>원장+감사+step log+outbox 기록"]
        qry["WalletQuery / LedgerQueryService<br/>+ WalletAccessPolicy"]
        relay["OutboxRelayService<br/>+ RelayMonitoringService"]
        cons["OutboxConsumerService<br/>+ ConsumerMonitoring · Pruning"]
        alertsvc["OperationalAlertService"]
        ports["포트 인터페이스<br/>Wallet·Relay·Consumer Repository /<br/>OutboxPublisher / AlertPublisher / RelayMetricsRecorder"]
    end

    model["domain 계층<br/>WalletAccount · Money · LedgerEntry · AuditEvent ·<br/>OperationStepLog · OutboxEvent · RelayRun · Alert"]

    subgraph INFRA["infra 계층 — 아웃바운드 어댑터"]
        mem["InMemoryWalletRepository<br/>(!postgres 프로파일)"]
        jdbc["JdbcWalletRepository<br/>(postgres 프로파일, Flyway)"]
        pub["InMemory / Http<br/>OutboxPublisher"]
        slack["Slack / Noop<br/>AlertPublisher"]
    end

    db[("PostgreSQL")]

    user --> sec
    ops --> sec
    sec --> wapi
    sec --> oapi

    wapi --> cmd
    wapi --> qry
    oapi --> relay
    oapi --> cons
    oapi --> alertsvc

    cmd --> ports
    qry --> ports
    relay --> ports
    cons --> ports
    alertsvc --> ports

    ports -.->|도메인 모델 사용| model
    ports -->|구현| mem
    ports -->|구현| jdbc
    ports -->|구현| pub
    ports -->|구현| slack
    jdbc --> db

    classDef actor fill:#fff3cd,stroke:#b45309,stroke-width:2px
    classDef apiLayer fill:#dbeafe,stroke:#1d4ed8
    classDef applLayer fill:#dcfce7,stroke:#16a34a
    classDef portNode fill:#bbf7d0,stroke:#15803d,stroke-width:2px
    classDef domainLayer fill:#fef9c3,stroke:#ca8a04,stroke-width:2px
    classDef infraLayer fill:#ede9fe,stroke:#7c3aed
    classDef storage fill:#e2e8f0,stroke:#475569

    class user,ops actor
    class sec,wapi,oapi apiLayer
    class cmd,qry,relay,cons,alertsvc applLayer
    class ports portNode
    class model domainLayer
    class mem,jdbc,pub,slack infraLayer
    class db storage
    style API fill:#eff6ff,stroke:#93c5fd
    style APPL fill:#f0fdf4,stroke:#86efac
    style INFRA fill:#f5f3ff,stroke:#c4b5fd
```

> 색상: 🟡 액터 · 🔵 api(인바운드) · 🟢 application(유스케이스, 진한 초록 = 포트) · 🟨 domain · 🟣 infra(아웃바운드) · ⚪ DB

- 돈 이동(충전·송금)은 하나의 operation으로 원장 + 감사 로그 + step log + outbox event를 남긴다(증적 원자성).
- 운영 API는 operator(조회)/admin(변경) 토큰과 접근 감사 필터로 보호된다.

## 3. 구동·관측 흐름 (스케줄러 → 지표 → Grafana)

스케줄러가 relay를 주기 구동하고, 지표는 카운터(기록 시점 증가)와 게이지(스크레이프 시점 조회) 두 경로로 Micrometer에 모여 Prometheus → Grafana로 흐른다.

```mermaid
graph LR
    sched["스케줄러<br/>(OutboxRelayScheduler 등)"] -->|주기 구동| relay["OutboxRelayService<br/>+ Monitoring"]
    relay -->|recordSuccess / recordFailure| reg["OutboxRelayMetricsRegistry<br/>(카운터)"]
    relay -.->|스크레이프 시점 조회| binder["OutboxMetricsBinder<br/>(게이지)"]
    reg --> mm["Micrometer<br/>Registry"]
    binder --> mm
    mm --> act["/actuator/prometheus"]
    act -->|15s 스크랩| prom["Prometheus"]
    prom -->|PromQL| graf["Grafana<br/>ai-repo Overview"]

    classDef driver fill:#fff3cd,stroke:#b45309
    classDef service fill:#dcfce7,stroke:#16a34a
    classDef counter fill:#dbeafe,stroke:#1d4ed8
    classDef gauge fill:#fce7f3,stroke:#be185d
    classDef expose fill:#ede9fe,stroke:#7c3aed

    class sched driver
    class relay service
    class reg counter
    class binder gauge
    class mm,act expose
    class prom,graf expose
```

> 색상: 🟡 구동(스케줄러) · 🟢 서비스 · 🔵 카운터 경로(기록 시점) · 🩷 게이지 경로(스크레이프 시점) · 🟣 노출·수집

지표 목록과 대시보드 패널: `docs/development/k8s-local-monitoring.md` Outbox 커스텀 지표 섹션.

## 4. (opt-in) ELK 로그 파이프라인

기본 로그 스택은 PLG(Loki)이며, 학습용으로 **opt-in** ELK 스택을 병존시킬 수 있다. ArgoCD(GitOps)에는 포함하지 않고 `scripts/elk-local-up.sh`로만 수동 기동한다. Filebeat(DaemonSet)가 `ai-repo` 파드 로그를 tail → Logstash가 grok으로 Spring 로그를 파싱 → Elasticsearch 역색인(`spring-logs-YYYY.MM.dd`) → Kibana Discover에서 KQL 검색한다.

```mermaid
graph LR
    pods["ai-repo 파드 로그<br/>/var/log/containers/*ai-repo*.log"]

    subgraph LOGGING["logging 네임스페이스 (opt-in · ArgoCD 미포함)"]
        fb["Filebeat<br/>DaemonSet (노드별 tail)"]
        ls["Logstash<br/>grok 파싱 :5044"]
        es[("Elasticsearch<br/>역색인 :9200 / NodePort 30920<br/>index spring-logs-YYYY.MM.dd")]
        kb["Kibana<br/>Discover/KQL :5601 / NodePort 30561"]
    end

    dev["개발자<br/>localhost:30561"]

    pods -->|container input| fb
    fb -->|beats| ls
    ls -->|bulk index| es
    kb -->|query| es
    dev -->|브라우저| kb

    classDef source fill:#e2e8f0,stroke:#475569
    classDef optin fill:#fce7f3,stroke:#be185d
    classDef store fill:#fef9c3,stroke:#ca8a04,stroke-width:2px
    classDef actor fill:#fff3cd,stroke:#b45309,stroke-width:2px

    class pods source
    class fb,ls,kb optin
    class es store
    class dev actor
    style LOGGING fill:#fdf2f8,stroke:#f9a8d4,stroke-dasharray:5 5
```

> 색상: ⚪ 로그 소스 · 🩷 ELK 컴포넌트(opt-in, 점선 = ArgoCD 미포함 수동 기동) · 🟨 Elasticsearch 역색인 · 🟡 개발자

- grok 파싱에 실패해도 원본 `message`는 유지되어 ES로 전달된다(`_grokparsefailure_spring` 태그).
- 기동/제거·접속 URL·KQL 예시: `docs/development/elk-local-logging.md`, 개념 학습: `docs/learning/elk-stack.md`, 결정 배경: `docs/adr/0066-elk-logging-stack.md`.

## 관련 문서

- `docs/development/k8s-local-monitoring.md` — 접속 정보·명령어·지표·로그 검색
- `docs/development/ci-cd-gitops.md` — 배포 파이프라인·트러블슈팅
- `docs/adr/0059-k8s-deploy-and-observability.md` — 설계 결정
