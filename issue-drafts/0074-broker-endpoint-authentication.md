# /internal/broker/outbox-events 인증(shared secret) 및 네트워크 전제 명시

## 배경

`POST /internal/broker/outbox-events`(`OperationOutboxConsumerController`)는 header-body 일치 검증만 있고 인증이 없었다. 이 endpoint는 앱과 같은 포트 표면(로컬 NodePort 30080)을 공유하므로, relay/broker 뿐 아니라 같은 네트워크에서 접근 가능한 누구나 consumer로 임의 outbox event를 주입할 수 있었다. `SecurityConfig`에서 `/internal/broker/**`는 어떤 `securityMatcher`에도 걸리지 않아 default chain의 `permitAll`로 떨어졌다.

## 문제

- 미인증 호출자가 임의 `outboxEventId`/payload로 consumer 부작용(receipt 적재 등)을 유발할 수 있다.
- publisher(`HttpOperationOutboxPublisher`)는 계약 헤더(`X-Outbox-Event-Id` 등)만 붙이고 인증 헤더가 없어, 인증 도입 시 publisher도 함께 고쳐야 과도기 401을 피한다.

## 결정 / 제안

- broker→consumer shared secret 헤더 `X-Broker-Token` 도입. consumer가 `ai-repo.outbox.consumer.broker-token`(env `AI_REPO_OUTBOX_CONSUMER_BROKER_TOKEN`, 로컬 기본 `local-broker-token`)과 `MessageDigest.isEqual` 상수시간 비교. 불일치/누락 → 401.
- `/internal/broker/**` 전용 `SecurityFilterChain`(Order 3) + `BrokerTokenAuthenticationFilter`(`OncePerRequestFilter`)를 신설. admin 운영 API의 filter/chain/error-handler 축을 재사용. 401은 `BrokerSecurityErrorHandler`가 `{"code","message","timestamp"}` 형태로 작성.
- publisher가 같은 token(`ai-repo.outbox.publisher.http.broker-token`, env `AI_REPO_OUTBOX_PUBLISHER_HTTP_BROKER_TOKEN`)을 `X-Broker-Token`으로 부착. contract test로 고정.
- NetworkPolicy 전제: shared secret은 방어의 한 축이며 `/internal/**`는 클러스터 내부 relay만 도달하도록 ingress/NetworkPolicy로 막는 것을 전제(토큰은 2차 방어선).

## 완료 조건

- [x] broker→consumer shared secret 헤더(env 주입) 검증 추가 + 미인증 401 테스트
- [x] HTTP publisher가 같은 secret을 부착하도록 계약(contract test) 갱신
- [x] ADR/문서에 NetworkPolicy 전제 또는 인증 방식 명시

## 검증 명령

```bash
./gradlew test scenarioTest postgresScenarioTest
scripts/check-dev-rules.sh
```

## 후속 (main 머지 후)

- 다중 토큰(신·구) 동시 수용으로 무중단 로테이션 지원(현재 단일 토큰, 로테이션 순단 수용).
- mTLS/서명 기반 상호 인증(로컬 학습 스택 범위 밖).

관련: GitHub Issue #123, 후속 #134, ADR-0065, progress 0074.
