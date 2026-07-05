# Stale Cleanup Artifacts

## 목표

Ponytail audit에서 확인된 저위험 cleanup 항목을 먼저 제거한다.

## 완료

- `spring-security-oauth2-jose` 직접 의존성을 제거했다.
- 완료된 end-user JWT 구현 계획 문서를 삭제했다.
- 존재하지 않는 `skills/` 루트를 스캔하던 `scripts/sync-codex-skills.py`를 삭제했다.
- README와 project-skills 문서를 현재 `project-skills/` 직접 관리 방식으로 갱신했다.

## 검증

- `./gradlew compileJava`
- `./gradlew dependencyInsight --dependency spring-security-oauth2-jose --configuration runtimeClasspath`
- `scripts/check-dev-rules.sh`

## 남은 일

- 더 큰 ponytail 항목(`InMemoryWalletRepository`, issue-drafts 정리, JDBC composite 제거)은 별도 작업으로 분리한다.
