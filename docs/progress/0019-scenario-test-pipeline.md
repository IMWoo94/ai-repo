# 0019. Scenario Test Pipeline

## 스펙 목표

- 현재 개발 기준선을 리뷰하고 개선점을 문서화한다.
- 대표 사용자/운영 흐름을 시나리오 테스트로 고정한다.
- CI에서 일반 검증과 시나리오 검증을 분리한다.

## 완료 결과

- 현재 개발 기준선 리뷰 문서를 추가했다.
- ADR-0022로 시나리오 테스트 파이프라인 결정을 기록했다.
- `scenarioTest` Gradle task를 추가했다.
- GitHub Actions에 `Scenario Test` job을 추가했다.
- 돈 이동 증거 흐름과 outbox 운영 흐름 시나리오 테스트를 추가했다.
- 시나리오 테스트 작성 기준 문서를 추가했다.

## 검증

- `./gradlew test`로 일반 테스트가 scenario tag를 제외하고 통과하는지 확인한다.
- `./gradlew scenarioTest`로 시나리오 테스트가 별도 실행되는지 확인한다.
- `./gradlew check`로 기존 품질 게이트가 유지되는지 확인한다.
- `docker compose config`와 `git diff --check`를 확인한다.

## 남은 일

- PostgreSQL profile 기반 scenario test는 아직 없다.
- 실제 broker publish scenario는 broker adapter 도입 후 추가한다.
- 인증/인가 기반 관리자 scenario는 후속 작업이다.

## 관련 문서

- `docs/reviews/current-development-baseline-review.md`
- `docs/testing/scenario-test-strategy.md`
- `docs/adr/0022-scenario-based-test-pipeline.md`
- `src/test/java/com/imwoo/airepo/wallet/scenario/WalletScenarioFlowTest.java`
