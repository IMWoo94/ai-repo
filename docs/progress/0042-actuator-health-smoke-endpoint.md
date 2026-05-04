# 0042. Actuator Health Smoke Endpoint

## 스펙 목표

- 릴리스 시연과 smoke 검증에서 사용할 표준 health endpoint를 추가한다.
- `/actuator/health`는 운영 admin header 없이도 로컬 smoke에서 확인 가능해야 한다.
- MVP local smoke script가 domain API뿐 아니라 application health도 확인하게 한다.

## 완료 결과

- `spring-boot-starter-actuator` 의존성을 추가했다.
- `management.endpoints.web.exposure.include=health` 설정을 추가했다.
- `/actuator/health`가 `UP`을 반환하는 backend test를 추가했다.
- `scripts/mvp-local-smoke.sh`가 Actuator health 응답을 검증하게 했다.
- local test guide와 unreleased release candidate notes를 갱신했다.

## 검증

- `./gradlew test --tests '*ActuatorHealthEndpointTest'`
- `scripts/mvp-local-smoke.sh`
- `git diff --check`

## 남은 일

- readiness/liveness probe를 실제 배포 환경과 연결할지는 배포 방식이 정해진 뒤 결정한다.
- 운영자 relay health/pruning 화면은 별도 UI 작업으로 남긴다.

## 관련 문서

- `build.gradle`
- `src/main/resources/application.yml`
- `scripts/mvp-local-smoke.sh`
- `docs/testing/local-test-guide.md`
- `issue-drafts/0042-actuator-health-smoke-endpoint.md`
