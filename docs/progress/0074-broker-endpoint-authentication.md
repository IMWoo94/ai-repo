# 0074. Broker Consumer Endpoint Authentication

## 스펙 목표

- 미인증 상태였던 `POST /internal/broker/outbox-events`(`OperationOutboxConsumerController`)에 broker→consumer shared secret 헤더 인증을 추가한다.
- HTTP publisher(`HttpOperationOutboxPublisher`)가 같은 secret을 부착하도록 계약을 갱신한다.
- 미인증/오토큰 401, 정상 토큰 2xx, publish→consume loop 유지를 회귀 테스트로 고정한다.

## 완료 결과

- `X-Broker-Token` 헤더를 도입했다. consumer는 `ai-repo.outbox.consumer.broker-token`(env `AI_REPO_OUTBOX_CONSUMER_BROKER_TOKEN`, 로컬 기본 `local-broker-token`)과 `MessageDigest.isEqual` 상수시간 비교로 검증한다.
- `/internal/broker/**` 전용 `SecurityFilterChain`(Order 3)을 신설하고 `BrokerTokenAuthenticationFilter`(`OncePerRequestFilter`)를 체인에 등록했다. 기존 default chain은 Order 4→5로 밀었다. admin 운영 API의 filter/chain/error-handler 축을 그대로 따랐다.
- `BrokerSecurityErrorHandler`가 admin과 동일한 JSON error 형태(`{"code","message","timestamp"}`)로 `BROKER_AUTHENTICATION_REQUIRED` 401을 작성한다.
- `HttpOperationOutboxPublisher`에 broker token 생성자 인자(`ai-repo.outbox.publisher.http.broker-token`, env `AI_REPO_OUTBOX_PUBLISHER_HTTP_BROKER_TOKEN`)를 추가하고 `X-Broker-Token` 헤더를 부착한다.
- `BrokerTokenGuard`(`JwtSecretGuard` 동일 패턴)를 추가해, 배포 프로파일(`postgres`/`prod`)에서 consumer 토큰이 커밋된 기본값 `local-broker-token`이면 startup fail-fast(그 외 프로파일은 경고). 커밋된 기본 secret으로의 fail-open을 막는다.
- 회귀 테스트: 필터 유닛 테스트(비-broker path 통과, 미토큰 401, 오토큰 401, 정상 토큰 통과), controller 미토큰/오토큰 401 + 정상 토큰 2xx, contract test의 `X-Broker-Token` 헤더 부착 단언, publish→consume loop 유지.

## 개선 건수

1. `/internal/broker/outbox-events` 미인증 event 주입 차단(shared secret 게이팅).
2. publisher가 같은 secret을 부착하도록 계약 갱신.

## 검증

- `./gradlew test` 통과
- `./gradlew scenarioTest` (broker endpoint를 거치는 loop 포함)

## 남은 일

- 다중 토큰(신·구) 동시 수용으로 무중단 로테이션 지원(현재 단일 토큰, 로테이션 순단 수용).
- mTLS/서명 기반 상호 인증은 범위 밖(로컬 학습 스택에는 과함).

## 관련 문서

- `docs/adr/0065-broker-endpoint-authentication.md`
- `docs/releases/unreleased.md`
