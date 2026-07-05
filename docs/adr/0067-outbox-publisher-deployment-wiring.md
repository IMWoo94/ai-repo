# ADR-0067: Outbox Publisher Deployment Wiring

## 상태

Accepted

## 배경

배포(`postgres`) 프로파일에서 outbox relay가 event를 발행하지만 컨슈머가 이를 수신하지 못하고, 재시작 시 유실되는 결함이 있었다. 두 결함이 겹쳐 있었다.

1. **memory 기본값 무동작**: `deploy/k8s/app.yaml`이 `AI_REPO_OUTBOX_PUBLISHER_TYPE`를 주입하지 않아 `application.yml` 기본값 `memory`가 선택된다. 이때 `InMemoryOperationOutboxPublisher`(`matchIfMissing=true`)가 발행을 담당하는데, relay는 event를 `PUBLISHED`로 마킹하지만 실제로는 in-memory 큐에만 쌓여 컨슈머(`POST /internal/broker/outbox-events`)로 전달되지 않는다. 프로세스 재시작 시 큐 내용이 사라져 event가 유실된다.
2. **endpoint 경로 불일치**: `http` 모드로 전환하더라도 기본 endpoint 경로가 `http://127.0.0.1:18080/outbox-events`로, 실제 컨슈머 매핑 `/internal/broker/outbox-events`(`OperationOutboxConsumerController`)와 어긋난다. broker-token(ADR-0065)은 이미 배포 secret으로 주입되지만, 경로가 맞지 않으면 발행이 404로 실패한다.

## 결정

배포 프로파일에서 outbox relay가 자기 서비스의 컨슈머 endpoint로 실제 HTTP 발행을 하도록 배선을 명시하고, http 모드 기본 경로를 컨슈머 매핑과 정합시킨다.

| 항목 | 결정 |
| --- | --- |
| 배포 publisher 타입 | `deploy/k8s/app.yaml`에 `AI_REPO_OUTBOX_PUBLISHER_TYPE=http` env 주입. memory 기본값 대신 HTTP publisher(`HttpOperationOutboxPublisher`)를 선택 |
| 배포 endpoint | `AI_REPO_OUTBOX_PUBLISHER_HTTP_ENDPOINT=http://ai-repo:8080/internal/broker/outbox-events` 주입. Service 이름 `ai-repo`, 포트 8080으로 앱이 자기 자신의 컨슈머 endpoint에 발행해 publish→consume loop를 완성 |
| 기본 경로 정합 | `application.yml`의 http 기본 endpoint 경로를 `/outbox-events` → `/internal/broker/outbox-events`로 변경(host/port `127.0.0.1:18080`는 유지). type만 http로 켰을 때도 컨슈머 매핑과 일치 |
| broker-token | 이미 secret으로 주입되는 `AI_REPO_OUTBOX_PUBLISHER_HTTP_BROKER_TOKEN`(ADR-0065)을 그대로 사용. 추가 변경 없음 |

## 트레이드오프

### 장점

- java 변경 없이 배포 매니페스트 env 2줄 + 기본 경로 1줄로 event 유실 회귀를 닫는다.
- 앱이 자기 Service endpoint로 발행하므로 별도 broker 인프라 없이 배포 환경에서 publish→consume loop가 완성된다.
- http 기본 경로가 컨슈머 매핑과 일치해, 이후 다른 배포/로컬 환경에서 type만 http로 켜도 경로 불일치 함정이 사라진다.

### 비용

- 배포 프로파일에서 publisher type이 여전히 env 주입에 의존한다. env가 누락되면 다시 memory 기본값으로 조용히 무동작한다(가드 없음).
- 앱이 자기 자신에게 HTTP 호출을 하므로, 컨슈머 endpoint가 준비되기 전(startup 초기)의 발행은 실패·재시도에 의존한다.

## 대안

- **배포 프로파일 fail-fast 가드**: `postgres`/`prod` 프로파일에서 publisher type이 `memory`이면 startup을 실패시키는 가드(`BrokerTokenGuard` 패턴). 조용한 무동작을 원천 차단하지만 java 변경이 필요하고 로컬/테스트 프로파일과의 경계 설계가 필요하다. 남은 일로 기록한다.
- **application.yml 기본값을 http로 변경**: 배포뿐 아니라 모든 프로파일 기본이 바뀌어 로컬/테스트에서 예기치 않은 HTTP 발행이 발생한다. 배포에만 국한하는 env 주입이 영향 범위가 작다.
- **경로 불일치를 컨슈머 매핑 변경으로 해결**: 컨슈머 매핑(`/internal/broker/outbox-events`)은 broker 인증 체인(ADR-0065)과 결합돼 있어, publisher 기본값을 맞추는 것이 영향이 작다.
