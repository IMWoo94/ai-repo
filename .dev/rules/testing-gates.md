---
category: pipeline
triggers:
  - "src/main/java/**/*.java"
  - "src/main/resources/db/migration/*.sql"
  - "frontend/src/**/*.tsx"
  - "frontend/e2e/**/*.ts"
depends_on:
  - documentation-sync
---

# Testing Gates Rule

코드 변경은 같은 PR 안에서 자동 검증 가능한 테스트를 포함한다.

## Checklist

- `src/main/java` 변경은 관련 `src/test/java` 테스트를 포함한다.
- DB migration이나 JDBC 저장소 변경은 repository 또는 PostgreSQL scenario 검증을 포함한다.
- React 화면 변경은 `frontend/src/App.test.tsx` 또는 인접 component test를 포함한다.
- 사용자/운영자 브라우저 흐름 변경은 `frontend/e2e/` 테스트를 포함한다.
- 최종 검증은 `./gradlew check`, `./gradlew scenarioTest`, `./gradlew postgresScenarioTest`, frontend unit/build/e2e 중 영향 범위에 맞게 실행한다.
