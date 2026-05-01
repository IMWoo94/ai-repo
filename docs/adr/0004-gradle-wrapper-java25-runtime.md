# ADR-0004: Gradle Wrapper for Java 25 Runtime

## 상태

Accepted

## 맥락

ADR-0003에서는 초기 Gradle 기준을 `8.14+`로 두었다. 이후 IntelliJ에서 실제 프로젝트 import와 테스트 실행을 시작하면서 Java 25 런타임과 Gradle 실행 호환성이 더 중요한 제약으로 드러났다.

Spring Boot 4.x는 Gradle 8.14+와 9.x를 지원한다. 하지만 Java 25로 Gradle 자체를 실행하는 환경에서는 Gradle 9.x를 사용하는 편이 더 명확하다.

또한 `spring-boot-starter-webmvc-test` 의존성에서 버전이 비어 있는 오류가 발생했다. Spring Boot 공식 Gradle 문서는 버전 없는 starter 의존성 관리를 위해 `io.spring.dependency-management` plugin 또는 Gradle BOM 방식을 사용하라고 안내한다.

## 선택지

### 선택지 A: Gradle Wrapper 없이 IntelliJ 로컬 Gradle 사용

장점:

- 설정 파일이 적다.
- 사용자가 로컬에서 원하는 Gradle을 선택할 수 있다.

단점:

- 개발자마다 Gradle 버전이 달라진다.
- Java 25 실행 호환성 문제가 재현되기 어렵다.
- PR의 검증 기준이 불명확해진다.

### 선택지 B: Gradle 8.14.x Wrapper 유지

장점:

- ADR-0003의 초기 결정과 가깝다.
- Spring Boot 4.x 지원 범위 안에 있다.

단점:

- Java 25로 Gradle 자체를 실행하는 환경에서 호환성 확인 부담이 남는다.
- IntelliJ 환경에서 같은 문제가 반복될 수 있다.

### 선택지 C: Gradle 9.3.0 Wrapper 사용

장점:

- Java 25 런타임 기준과 더 잘 맞는다.
- 개발자와 CI가 같은 Gradle 버전을 사용한다.
- IntelliJ import와 CLI 검증 기준이 일치한다.

단점:

- Gradle 9.x 관련 플러그인 호환성 이슈가 생길 수 있다.
- ADR-0003의 Gradle 8.14+ 시작 기준을 일부 대체한다.

## 결정

Gradle Wrapper는 `9.3.0`을 사용한다.

의존성 관리는 `io.spring.dependency-management` plugin을 적용해 Spring Boot BOM 기반으로 처리한다.

## 결과

장점:

- IntelliJ와 CLI가 같은 Gradle 버전을 사용한다.
- Java 25 기준 실행 호환성을 명확히 한다.
- 버전 없는 Spring Boot starter 의존성이 BOM으로 관리된다.

비용:

- Gradle 9.x 관련 호환성 이슈가 생기면 별도 조정이 필요하다.
- ADR-0003의 Gradle 8.14+ 기준은 스캐폴딩 초기 판단으로 남고, 실제 Wrapper 기준은 이 ADR이 우선한다.

## 후속 작업

- IntelliJ에서 Gradle Wrapper 사용을 확인한다.
- `./gradlew test` 결과를 PR에 기록한다.
- CI 추가 시 Gradle Wrapper를 기준으로 실행한다.
