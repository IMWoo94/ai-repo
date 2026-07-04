# ADR-0063: Application Layer Spring Annotation Policy

## 상태

Accepted

## 배경

`application` 패키지는 지갑 명령/조회, outbox relay/consumer, 운영 로그 pruning, health policy 같은 use case와 port를 담는다. 현재 구현은 순수 Java application layer와 Spring wiring을 완전히 분리하지 않고, 일부 service와 policy 객체에 `@Service`, `@Component`, `@Value`, `@Transactional`을 사용한다.

Issue #125는 `JdbcWalletRepository` 분해와 함께 레이어 규칙을 구조 테스트로 고정해야 한다. 이때 application layer의 Spring annotation 허용 범위를 먼저 결정하지 않으면 ArchUnit 규칙이 실제 설계와 충돌한다.

## 결정

현재 단계에서는 application layer의 제한적 Spring annotation 사용을 허용한다.

| 항목 | 결정 |
| --- | --- |
| Service wiring | use case service는 `@Service`를 사용할 수 있다. |
| Policy/properties wiring | 설정값 기반 policy/properties 객체는 `@Component`와 생성자 `@Value`를 사용할 수 있다. |
| Transaction boundary | 여러 repository port 호출을 하나의 use case 원자성으로 묶어야 하는 application service는 `@Transactional`을 사용할 수 있다. |
| 금지 의존성 | application layer는 `api`, `infra`, `config`, Spring Web/JDBC/Security 구현 세부사항에 의존하지 않는다. |
| 검증 | ArchUnit 테스트로 domain/application/api/infra의 핵심 의존 방향을 고정한다. |

## 대안

### 선택지 A: application layer에서 Spring annotation을 모두 제거한다

순수 use case를 만들 수 있지만 현재 서비스/정책 객체 wiring을 별도 configuration으로 모두 옮겨야 한다. 이 변경은 #125의 persistence 분해보다 범위가 커지고, 기존 테스트와 component scan 구조를 크게 흔든다.

### 선택지 B: 제한적 Spring annotation을 허용하고 외부 adapter 의존만 금지한다

현재 구조와 맞고 변경 범위가 작다. 대신 application layer가 완전히 framework-free는 아니므로, 장기적으로 순수 use case 모듈화가 필요해지면 새 ADR로 전환해야 한다.

### 선택지 C: annotation 정책을 정하지 않고 ArchUnit만 도입한다

규칙이 모호해져 나중에 Spring annotation 사용을 둘러싼 drift를 막지 못한다.

## 결과

- application layer는 Spring DI/transaction annotation을 제한적으로 사용할 수 있다.
- domain layer는 Spring과 outer layer 의존을 갖지 않는다.
- application layer는 adapter 구현인 `api`, `infra`, `config`를 모른다.
- 구조 테스트는 현재 정책을 기준으로 작성한다.

## 후속 작업

- application layer를 framework-free use case로 분리할 필요가 생기면 이 ADR을 대체하는 새 ADR을 작성한다.
- 신규 Spring dependency가 application layer에 들어올 때는 ArchUnit 규칙 또는 이 ADR의 허용 범위를 함께 검토한다.
