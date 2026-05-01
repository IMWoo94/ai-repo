# ADR-0003: Java, Spring Boot, Gradle Baseline

## 상태

Accepted

## 맥락

이 저장소는 Java 25와 Spring Boot 기반으로 핀테크 서비스를 학습한다. 목표는 최신 기술을 무조건 쓰는 것이 아니라, 장기 학습 기준과 근거 있는 트레이드오프를 남기는 것이다.

초기 스캐폴딩 전에 다음 기준을 결정해야 한다.

- Java 버전
- Spring Boot 버전 축
- Gradle 버전 축
- 안정성과 최신성 사이의 선택

2026-05-01 기준 공식 문서 확인 결과는 다음과 같다.

- Oracle Java 릴리스 표는 Java 25를 LTS로 표시하고 지원 종료 시점을 2033-09로 제시한다.
- Oracle Java 다운로드 페이지는 JDK 25를 최신 LTS 릴리스로 안내한다.
- Spring Boot 4.0.5 시스템 요구사항은 Java 17 이상과 Java 26까지의 호환성을 명시하고, Gradle 8.14 이상 또는 9.x를 지원한다.
- Spring Boot 3.5.x 문서 축은 Java 17 이상을 요구하지만 Java 25 호환 기준으로는 Spring Boot 4.x보다 보수적이다.

참고:

- https://www.java.com/releases/
- https://www.oracle.com/java/technologies/downloads/
- https://docs.spring.io/spring-boot/system-requirements.html
- https://docs.spring.io/spring-boot/3.5-SNAPSHOT/installing.html

## 선택지

### 선택지 A: Java 21 + Spring Boot 3.5.x + Gradle 8.x

장점:

- 기존 생태계와 예제 자료가 많다.
- Spring Framework 6.x와 Jakarta EE 10 기반이 상대적으로 안정적이다.
- 라이브러리 호환성 리스크가 낮다.

단점:

- 사용자가 명시한 Java 25 학습 목표와 어긋난다.
- 최신 LTS 기준의 학습 효과가 줄어든다.
- 이후 Java 25로 올리는 업그레이드 작업이 별도로 필요하다.

### 선택지 B: Java 25 + Spring Boot 3.5.x + Gradle 8.x

장점:

- Java 25 학습 목표를 충족한다.
- Spring Boot 3.x 생태계의 안정성을 일부 유지할 수 있다.

단점:

- Spring Boot 3.5.x의 공식 문서 축은 Java 25보다 낮은 호환 범위를 전제로 설명되는 경우가 있다.
- 최신 Java와 상대적으로 오래된 Spring 축을 조합하면서 애매한 호환성 검증 비용이 생긴다.
- Spring Boot 4.x로 갈 가능성이 높다면 초기부터 업그레이드 비용이 생긴다.

### 선택지 C: Java 25 + Spring Boot 4.x + Gradle 9.x

장점:

- Java 25 LTS 학습 목표와 가장 잘 맞는다.
- Spring Boot 4.x의 공식 시스템 요구사항이 Java 26까지의 호환성을 명시한다.
- Spring Framework 7.x와 최신 Servlet/Jakarta 축을 학습할 수 있다.
- Gradle 9.x를 통해 최신 빌드 도구 흐름을 함께 학습할 수 있다.

단점:

- 일부 라이브러리와 예제의 Spring Boot 4.x 대응이 늦을 수 있다.
- Spring Framework 7.x, Servlet 6.1, Tomcat 11 축에서 기존 Spring Boot 3.x 자료와 차이가 생긴다.
- 문제 해결 시 참고 자료가 Spring Boot 3.x보다 적을 수 있다.

### 선택지 D: Java 25 + Spring Boot 4.x + Gradle 8.14+

장점:

- Java 25와 Spring Boot 4.x 학습 목표를 충족한다.
- Gradle 9.x보다 플러그인 호환성 리스크를 낮출 수 있다.
- Spring Boot 4.x가 명시적으로 지원하는 Gradle 하한선을 따른다.

단점:

- Gradle 9.x 학습은 뒤로 밀린다.
- 장기적으로 Gradle 9.x 전환 ADR이 필요할 수 있다.

## 결정

초기 기준은 `Java 25 + Spring Boot 4.x + Gradle 8.14+`로 한다.

구체 정책:

| 항목 | 결정 |
| --- | --- |
| Java | Java 25 |
| Spring Boot | 4.x 최신 안정 릴리스 |
| Gradle | 8.14 이상으로 시작 |
| Gradle 9.x | 초기 스캐폴딩 이후 플러그인 호환성을 확인한 뒤 별도 전환 |
| 빌드 관리 | Gradle Wrapper를 커밋 |
| 버전 근거 | 공식 문서 링크를 ADR과 README에 유지 |

## 결정 이유

Java 25는 사용자의 명시 목표이며, Oracle 문서상 최신 LTS 축이다. 따라서 Java 21로 낮추는 것은 학습 목표에 맞지 않는다.

Spring Boot는 4.x를 선택한다. 공식 시스템 요구사항이 Java 26까지의 호환성을 명시하므로 Java 25 학습 기준과 잘 맞는다. Spring Boot 3.5.x는 안정성 장점이 있지만, Java 25와 장기 학습 기준을 맞추려면 4.x가 더 일관적이다.

Gradle은 9.x가 아니라 8.14+로 시작한다. Spring Boot 4.x가 Gradle 8.14+와 9.x를 모두 지원하므로, 초기에는 플러그인 호환성 리스크를 낮추고 애플리케이션 스캐폴딩과 테스트 전략에 집중한다. Gradle 9.x 전환은 프로젝트가 최소 기능과 CI를 갖춘 뒤 별도 ADR로 판단한다.

## 결과

장점:

- Java 25 학습 목표를 유지한다.
- Spring Boot 4.x 공식 호환 범위 안에서 시작한다.
- Gradle 9.x 관련 초기 변수를 줄인다.
- 이후 Gradle 9.x 전환 여부를 독립적인 결정으로 남길 수 있다.

비용:

- Spring Boot 4.x 생태계 이슈를 직접 만나게 될 수 있다.
- Spring Boot 3.x 기준 예제와 문서를 그대로 적용하기 어렵다.
- Gradle 9.x 전환 여부를 나중에 다시 검토해야 한다.

## 후속 작업

- Spring Initializr 또는 Gradle 수동 설정으로 Java 25/Spring Boot 4.x 프로젝트를 생성한다.
- Gradle Wrapper는 8.14 이상 버전으로 시작한다.
- `./gradlew test`와 `./gradlew check`가 동작하도록 CI를 구성한다.
- 첫 스캐폴딩 PR에서 실제 Java/Spring/Gradle 버전을 PR 본문에 기록한다.
- Gradle 9.x 전환은 플러그인 호환성 확인 후 별도 ADR로 판단한다.
