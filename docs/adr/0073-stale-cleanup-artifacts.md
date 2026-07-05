# ADR-0073: Stale Cleanup Artifacts

## 상태

Accepted

## 배경

Ponytail audit에서 완료된 구현 계획, 실제 소스 루트가 없는 스킬 동기화 스크립트, 중복 직접 의존성이 확인됐다.

## 결정

- 완료된 end-user JWT 구현 계획은 삭제하고 ADR/progress/spec만 남긴다.
- `scripts/sync-codex-skills.py`는 삭제한다. 현재 레포에는 스크립트가 읽는 `skills/` 루트가 없고, 프로젝트 스킬은 `project-skills/`에서 직접 관리한다.
- `spring-security-oauth2-jose` 직접 의존성은 제거한다. `spring-boot-starter-oauth2-resource-server`가 transitively 제공한다.

## 결과

- 완료된 agent plan과 죽은 스크립트가 release 후보 범위에서 빠진다.
- JWT resource server 런타임 의존성은 starter 하나로 유지한다.
- 새 기능이나 API 동작 변경은 없다.
