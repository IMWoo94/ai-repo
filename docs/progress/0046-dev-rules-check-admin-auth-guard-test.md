# Dev Rules Check and Admin Auth Guard Test

## 스펙 목표

- 코드, DB, 프론트, CI/script 변경이 문서와 테스트 갱신 없이 들어오는 상황을 CI에서 조기에 탐지한다.
- 운영 API 인증의 핵심 정책인 admin token과 operator id 검증을 controller slice보다 작은 단위 테스트로 고정한다.

## 완료 결과

- `scripts/check-dev-rules.sh`를 추가했다.
  - 변경 파일 기준으로 backend, DB migration, frontend, CI/script 변경의 문서 동반 여부를 확인한다.
  - unstaged/staged diff와 untracked file을 함께 스캔한다.
  - macOS 기본 Bash 3.x와 호환되도록 `mapfile`에 의존하지 않는다.
  - backend production 변경에는 `src/test/java` 테스트 동반을 요구한다.
  - frontend source 변경에는 component test 또는 E2E 동반을 요구한다.
  - DB migration 변경에는 ADR 동반을 요구한다.
- GitHub Actions에 `Dev Rules Check` job을 추가했다.
  - push event에서는 `github.event.before`를 base ref로 넘겨 main push에서도 no-op이 되지 않게 했다.
- `AdminAuthorizationGuardTest`를 추가해 다음 정책을 검증했다.
  - 유효한 admin token은 operator id를 trim해서 인증한다.
  - 누락/오류 admin token은 `AdminAuthenticationException`으로 거부한다.
  - token 검증 후 operator id 누락은 `AdminAuthorizationException`으로 거부한다.
- README, local test guide, unreleased release candidate note를 최신화했다.

## 검증

- `scripts/check-dev-rules.sh`
- `./gradlew test --no-daemon`
- `git diff --check`

## 남은 일

- dev rules가 실제 팀 운영에서 너무 엄격하거나 느슨한지 다음 PR들에서 관찰한다.
- 변경 성격이 더 세분화되면 `.dev/rules` 같은 선언형 rule 파일로 분리할지 검토한다.
