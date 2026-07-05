# Stale cleanup artifacts

## 배경

Ponytail audit에서 완료된 계획 문서, 죽은 스킬 동기화 스크립트, 중복 직접 의존성이 확인됐다.

## 범위

- `spring-security-oauth2-jose` 직접 의존성 제거
- 완료된 end-user JWT 구현 계획 삭제
- `scripts/sync-codex-skills.py` 삭제
- 관련 README/project-skills/progress/release/wiki 문서 정리

## 제외

- `InMemoryWalletRepository` 제거
- `issue-drafts/` 전체 정리
- JDBC composite 제거
- 운영 API path matcher 중복 제거

## 검증

- `./gradlew compileJava`
- `./gradlew dependencyInsight --dependency spring-security-oauth2-jose --configuration runtimeClasspath`
- `scripts/check-dev-rules.sh`
