# [Feature] Actuator health smoke endpoint 추가

## 배경

MVP 출시 준비 항목에 actuator 기반 health check가 남아 있다. 기존 local smoke script는 지갑 API, 운영자 API, 프론트 HTML을 확인하지만 표준 애플리케이션 health endpoint는 확인하지 않는다.

## 목표

- Spring Boot Actuator health endpoint를 추가한다.
- `/actuator/health`를 local smoke에서 확인한다.
- admin header 없이 health endpoint가 `UP`을 반환하는지 테스트한다.

## 범위

- `spring-boot-starter-actuator` 의존성 추가
- health endpoint 노출 설정 추가
- health endpoint test 추가
- `scripts/mvp-local-smoke.sh` 갱신
- local test guide, release candidate notes, progress 갱신

## 범위 제외

- actuator 전체 endpoint 공개
- 배포 환경 readiness/liveness 설정
- 운영자 relay health/pruning UI

## 인수 조건

- [x] `GET /actuator/health`가 200과 `UP`을 반환한다.
- [x] health endpoint는 admin header 없이 조회 가능하다.
- [x] MVP local smoke script가 actuator health를 검증한다.
- [x] 운영 API admin auth contract는 변경하지 않는다.

## 검증

- `./gradlew test --tests '*ActuatorHealthEndpointTest'`
- `scripts/mvp-local-smoke.sh`
- `git diff --check`

## 리스크와 트레이드오프

- health endpoint만 노출한다. metrics, env, beans 같은 endpoint는 MVP smoke 범위 밖이다.
- 실제 배포 probe 설정은 배포 환경이 정해진 뒤 별도 결정한다.
