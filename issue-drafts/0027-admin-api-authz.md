# Admin API Authz

## 배경

Outbox manual review와 requeue API는 운영자가 장애 event를 조회하고 재처리하는 행위다. 현재는 로컬 학습 API라 누구나 호출할 수 있는 상태이므로, 운영 행위 경계를 코드와 테스트로 먼저 고정해야 한다.

## 목표

- Outbox 운영 API에 인증/인가 guard를 추가한다.
- 로컬/포트폴리오 단계에서는 공유 admin token과 operator header를 사용한다.
- 인증 실패는 401, 권한 실패는 403으로 구분한다.
- requeue 감사 이력의 operator는 인증된 operator에서 가져온다.
- ADR, progress report, 테스트 문서를 갱신한다.

## 범위

- `GET /api/v1/outbox-events/manual-review`
- `POST /api/v1/outbox-events/{outboxEventId}/requeue`
- `GET /api/v1/outbox-events/{outboxEventId}/requeue-audits`

## 제외 범위

- JWT/OAuth2/OIDC 연동
- 사용자 로그인/회원 인증
- Spring Security 기반 세션/권한 모델 전체 도입
- 운영자 승인 워크플로우

## 완료 조건

- [ ] manual review 조회, requeue, requeue audit 조회가 운영 인증을 요구한다.
- [ ] 누락/잘못된 token은 401로 응답한다.
- [ ] 운영자 식별자 누락은 403으로 응답한다.
- [ ] requeue audit operator는 header의 인증 operator로 기록된다.
- [ ] 기존 사용자 지갑 API는 영향을 받지 않는다.
- [ ] `./gradlew test scenarioTest check`가 통과한다.

## 검증

```bash
./gradlew test scenarioTest check
```
