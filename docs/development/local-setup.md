# Local Setup

이 문서는 IntelliJ IDEA에서 `ai-repo` Spring Boot 프로젝트를 실행하기 위한 로컬 기준을 설명합니다.

## 기준 버전

ADR-0003 기준:

- Java 25
- Spring Boot 4.x
- Gradle 8.14+

## IntelliJ 설정

1. Java 25 SDK를 설치합니다.
2. IntelliJ에서 `File > Project Structure > Project SDK`를 Java 25로 설정합니다.
3. `build.gradle`을 Gradle 프로젝트로 import합니다.
4. Gradle JVM을 Java 25로 설정합니다.
5. IntelliJ가 사용할 Gradle은 Java 25 실행 호환성이 확인된 버전을 사용합니다.
   - 권장: Gradle 9.x
   - 대안: Gradle 8.14.4 이상을 사용하고 Java 25 실행 호환성을 별도로 확인
6. 테스트는 Gradle 또는 IntelliJ JUnit runner로 실행합니다.

## 검증 명령

Gradle이 설치되어 있으면 다음을 실행합니다.

```bash
gradle test
gradle check
```

Gradle Wrapper가 생성된 뒤에는 다음 명령을 기준으로 사용합니다.

```bash
./gradlew test
./gradlew check
```

## 현재 제약

현재 커밋에는 Gradle Wrapper를 포함하지 않습니다. 로컬 Gradle 또는 IntelliJ Gradle import를 사용해 먼저 프로젝트를 검증한 뒤, Java 25 실행 호환성이 확인된 Gradle 버전으로 Wrapper를 생성해 별도 커밋으로 추가합니다.
