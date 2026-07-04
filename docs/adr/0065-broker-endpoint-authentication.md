# ADR-0065: Broker Consumer Endpoint Authentication

## 상태

Accepted

## 배경

`POST /internal/broker/outbox-events`(`OperationOutboxConsumerController`)는 header-body 일치 검증만 있고 인증이 없었다. 이 endpoint는 앱과 같은 포트 표면(로컬 NodePort 30080)을 공유하므로, relay/broker 뿐 아니라 같은 네트워크에서 접근 가능한 누구나 consumer로 임의 outbox event를 주입할 수 있었다. `SecurityConfig`에서 `/internal/broker/**`는 어떤 `securityMatcher`에도 걸리지 않아 Order(4) default chain의 `permitAll`로 떨어졌다.

publisher 측(`HttpOperationOutboxPublisher`)은 이미 `X-Outbox-Event-Id` 등 계약 헤더만 붙였고 인증 헤더는 없었다.

## 결정

broker→consumer 사이에 shared secret 헤더(`X-Broker-Token`)를 도입한다.

| 항목 | 결정 |
| --- | --- |
| 인증 방식 | consumer가 `X-Broker-Token` 헤더를 configured `ai-repo.outbox.consumer.broker-token`(env `AI_REPO_OUTBOX_CONSUMER_BROKER_TOKEN`, 로컬 기본 `local-broker-token`)과 상수시간 비교. 불일치/누락 → 401 |
| 게이팅 위치 | `/internal/broker/**` 전용 `SecurityFilterChain`(Order 3)을 신설하고 `BrokerTokenAuthenticationFilter`(`OncePerRequestFilter`)를 체인에 등록. admin chain(`AdminHeaderAuthenticationFilter` + `AdminSecurityErrorHandler`) 패턴과 동일하게 구성 |
| 401 응답 | `BrokerSecurityErrorHandler`가 admin과 동일한 JSON error 형태(`{"code","message","timestamp"}`)로 `BROKER_AUTHENTICATION_REQUIRED` 401 작성 |
| publisher | `HttpOperationOutboxPublisher`가 같은 token(`ai-repo.outbox.publisher.http.broker-token`, env `AI_REPO_OUTBOX_PUBLISHER_HTTP_BROKER_TOKEN`)을 `X-Broker-Token` 헤더로 부착. contract test로 헤더 부착을 고정 |
| 토큰 비교 | `MessageDigest.isEqual`로 timing-safe 비교(admin token 검증과 동일) |

## 왜 controller가 아니라 filter/chain인가

controller에서 직접 검증하면 header-body 검증 로직과 인증 로직이 섞이고, 401 JSON 형태를 controller마다 다시 만들어야 한다. admin 운영 API가 이미 "전용 security chain + `OncePerRequestFilter` + 공통 error handler"로 401을 통일해 두었으므로, broker endpoint도 같은 축을 따르는 것이 일관적이고 side-effect 이전에 요청을 차단한다. 전용 chain(Order 3)을 둬서 `permitAll` default로 새는 것을 명시적으로 막는다.

## 네트워크 전제와 토큰 로테이션

- **NetworkPolicy 전제**: shared secret은 방어의 한 축일 뿐이다. `/internal/**`는 클러스터 내부 relay만 호출해야 하며, ingress/NetworkPolicy로 외부에서 `/internal/broker/**`가 도달하지 못하게 막는 것을 전제로 한다. 토큰은 "네트워크가 뚫렸을 때의 2차 방어선"이다.
- **기본 토큰 fail-fast**: 로컬 기본값 `local-broker-token`은 저장소에 커밋돼 있어 그대로 두면 2차 방어선이 무력화된다. `BrokerTokenGuard`(`JwtSecretGuard`와 동일 패턴)가 배포 프로파일(`postgres`/`prod`)에서 consumer 토큰이 이 기본값이면 startup을 실패시키고, 그 외 프로파일에서는 경고만 남긴다. 즉 배포 프로파일은 `AI_REPO_OUTBOX_CONSUMER_BROKER_TOKEN` 주입을 강제한다.
- **토큰 로테이션**: consumer(`AI_REPO_OUTBOX_CONSUMER_BROKER_TOKEN`)와 publisher(`AI_REPO_OUTBOX_PUBLISHER_HTTP_BROKER_TOKEN`)는 같은 값이어야 한다. 로테이션 시 무중단을 위해서는 consumer가 신·구 토큰을 동시에 수용하는 window가 필요하나, 현재는 단일 토큰만 지원하므로 로테이션은 짧은 순단(publisher→consumer 순차 배포)을 수용한다. 다중 토큰 수용은 후속 과제로 둔다.

## 트레이드오프

### 장점

- 미인증 event 주입을 닫으면서 relay→consumer 계약은 헤더 하나 추가로 최소 변경.
- admin 운영 API와 동일한 filter/chain/error-handler 축을 재사용해 인증·401 형태가 일관됨.

### 비용

- consumer와 publisher가 같은 토큰 값을 공유해야 하므로 설정 동기화 책임이 생긴다(로테이션 순단).
- shared secret은 대칭 비밀이라 유출 시 양쪽을 모두 교체해야 한다. mTLS 같은 상호 인증은 범위 밖으로 둔다.

## 대안

- controller에서 직접 검증: 가장 단순하나 401 형태 중복과 관심사 혼합.
- mTLS/서명 기반 인증: 유출 내성이 크지만 로컬 학습 스택에는 과하고 인증서 배포/로테이션 비용이 크다.
- NetworkPolicy만으로 차단(토큰 없음): 포트 표면 공유 상황에서 2차 방어선이 없어 이슈 요구를 충족하지 못한다.
