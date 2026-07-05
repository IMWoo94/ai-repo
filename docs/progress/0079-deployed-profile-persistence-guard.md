# 0079. Deployed Profile Persistence Guard

## 스펙 목표

- Issue #145의 배포 프로파일 in-memory 저장소 유실 위험을 fail-fast 가드로 닫는다.
- 배포 프로파일(`prod`/`postgres`) 단독 `prod` 기동이 `postgres` 없이 In-Memory 저장소를 로드하는 경로를 startup 실패로 전환한다.
- 결정과 대안을 ADR로 확정한다.

## 완료 결과

- `DeployedProfilePersistenceGuard`(`wallet/config`)를 신설했다. `DeployedProfiles.isActive(env)`가 true인데 활성 프로파일에 `postgres`가 없으면 `IllegalStateException`으로 기동을 실패시킨다.
- 기존 자격증명 가드(`JwtSecretGuard`/`OpsTokenGuard`/`BrokerTokenGuard`)와 동일한 package-private `@Component` + `Environment` 주입 패턴을 따랐다.
- `DeployedProfilePersistenceGuardTest`로 (a) `prod` 단독 → throws, (b) `postgres` → ok, (c) `prod`+`postgres` → ok, (d) `local` → ok, (e) 프로파일 없음 → ok 를 `MockEnvironment`로 고정했다.
- 결정을 ADR-0068로 기록하고 대안(InMemory `@Profile` 좁히기 / `DeployedProfiles` 술어 통일)과 채택 근거를 서술했다.
- ADR index, progress index, unreleased release note에 변경 내용을 반영했다.

## 검증

- `./gradlew compileTestJava`
- `./gradlew test --tests '*DeployedProfilePersistenceGuardTest'`
- `AI_REPO_DEV_RULES_BASE=origin/main bash scripts/check-dev-rules.sh`

## 남은 일

- 배포 판정(`DeployedProfiles`)과 영속성 프로파일(`@Profile`)을 한 축으로 구조적으로 통일하는 리팩터링은 회귀 범위가 넓어 후속으로 둔다.

## 관련 문서

- `docs/adr/0068-deployed-profile-persistence-guard.md`
- `docs/releases/unreleased.md`
- `issue-drafts/0079-deployed-profile-persistence-guard.md`
