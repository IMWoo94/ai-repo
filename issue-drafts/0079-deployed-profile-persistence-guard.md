# prod 프로파일 단독 기동 시 In-Memory 저장소 로드(데이터 유실)

## 증상

`DeployedProfiles.isActive`는 `prod`/`postgres`를 배포로 취급하지만, 영속성은 `postgres`에만 반응한다(`JdbcWalletRepository @Profile("postgres")`, `InMemoryWalletRepository @Profile("!postgres")`). 그래서 `prod` 단독 기동은 자격증명 가드는 통과하되 In-Memory 저장소를 로드하고, 재시작 시 지갑 잔액이 유실된다.

## 결정 / 제안

- `DeployedProfilePersistenceGuard`(`wallet/config`)를 신설한다. `DeployedProfiles.isActive(env)`가 true인데 활성 프로파일에 `postgres`가 없으면 `IllegalStateException`으로 startup을 실패시킨다.
- 기존 자격증명 가드(`JwtSecretGuard`/`OpsTokenGuard`/`BrokerTokenGuard`)와 동일한 package-private `@Component` + `Environment` 주입 패턴을 따른다. 콜사이트 변경은 없다.

## 완료 조건

- [x] 배포 프로파일에서 `postgres` 누락 시 기동 실패 가드 추가
- [x] `MockEnvironment` 기반 회귀 테스트(prod 단독/postgres/prod+postgres/local/no-profile)
- [x] ADR/문서에 결정과 대안(InMemory `@Profile` 좁히기 / `DeployedProfiles` 술어 통일) 기록

## 검증 명령

```bash
./gradlew test --tests '*DeployedProfilePersistenceGuardTest'
AI_REPO_DEV_RULES_BASE=origin/main bash scripts/check-dev-rules.sh
```

관련: GitHub Issue #145, ADR-0068, progress 0079.
