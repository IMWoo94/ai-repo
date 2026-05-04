# Spring Security Role Model

## 배경

운영 API는 `X-Admin-Token`과 `X-Operator-Id` header 기반 guard로 보호되고 있다. 이 방식은 학습용 로컬 운영 경계를 빠르게 고정하는 데 충분했지만, API별 역할 차이를 표현하기 어렵고 Spring Security 표준 인증/인가 흐름과 분리되어 있다.

금융/핀테크 운영 API는 조회, 재처리, pruning처럼 위험도가 다르다. 따라서 Spring Security 기반 role model로 전환해 `ROLE_OPERATOR`, `ROLE_ADMIN` 경계를 코드와 테스트에 남겨야 한다.

## 목표

- Spring Security 기반 운영 API 인증/인가 경계를 추가한다.
- 기존 `X-Admin-Token`, `X-Operator-Id` header contract는 유지한다.
- 운영 조회/재처리 API는 `ROLE_OPERATOR`를 요구한다.
- 운영 로그 pruning API는 `ROLE_ADMIN`을 요구한다.
- 기존 오류 응답 code인 `ADMIN_AUTHENTICATION_REQUIRED`, `ADMIN_AUTHORIZATION_DENIED`를 유지한다.
- admin access audit이 401/403 접근도 계속 기록되게 한다.

## 범위

- Spring Security dependency 추가
- stateless security filter chain 추가
- admin header authentication filter 추가
- role enum/model 추가
- controller 내부 guard 호출 제거 또는 축소
- 보안 오류 JSON 응답 처리 추가
- API/security 테스트 추가
- ADR, progress report, README, local guide 갱신

## 제외 범위

- JWT/OIDC 로그인
- 사용자 DB 기반 계정/권한 관리
- refresh token/session
- 세분화된 다중 operator token
- 외부 IAM 연동

## 완료 조건

- [x] 운영 API가 Spring Security filter chain에서 보호된다.
- [x] token 누락/오류는 401 `ADMIN_AUTHENTICATION_REQUIRED`를 반환한다.
- [x] token은 맞지만 operator가 없으면 403 `ADMIN_AUTHORIZATION_DENIED`를 반환한다.
- [x] 운영 조회/재처리 API는 `ROLE_OPERATOR` 권한으로 접근된다.
- [x] pruning API는 `ROLE_ADMIN` 권한으로 접근된다.
- [x] 일반 사용자 API는 인증 없이 접근된다.
- [x] admin access audit이 401/403 접근도 기록한다.
- [x] `./gradlew test scenarioTest check`가 통과한다.

## 검증

```bash
./gradlew test scenarioTest check
```
