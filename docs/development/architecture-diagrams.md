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
```

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
```

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
```

지표 목록과 대시보드 패널: `docs/development/k8s-local-monitoring.md` Outbox 커스텀 지표 섹션.

## 관련 문서

- `docs/development/k8s-local-monitoring.md` — 접속 정보·명령어·지표·로그 검색
- `docs/development/ci-cd-gitops.md` — 배포 파이프라인·트러블슈팅
- `docs/adr/0059-k8s-deploy-and-observability.md` — 설계 결정
