# [Quality] 시나리오 기반 테스트 파이프라인 구축

GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/39

## 배경

v0.6.0 기준선은 기능 단위 테스트와 CI check는 갖췄지만, 포트폴리오/운영 하네스 관점에서 “대표 사용자 흐름이 실제로 연결되어 동작하는가”를 별도 시나리오로 검증하는 파이프라인은 아직 약하다.

## 목표

- 현재 개발 기준선을 리뷰하고 개선점을 문서화한다.
- 핵심 핀테크 흐름을 시나리오 기반 테스트로 고정한다.
- CI에서 일반 check와 scenario test를 분리해 실행한다.
- 향후 QA가 시나리오를 추가할 수 있는 기준 문서를 만든다.

## 범위

- 개발 기준선 리뷰 문서 추가
- 시나리오 테스트 전략/문서 추가
- Gradle scenario test task 추가
- GitHub Actions scenario test job 추가
- 대표 시나리오 테스트 코드 추가

## 범위 제외

- 외부 브로커 연동
- E2E 브라우저 테스트
- 실제 배포 환경 smoke test
- 인증/인가 도입

## 수용 기준

- [ ] 현재 기준선의 강점/리스크/개선점이 문서화된다.
- [ ] `scenarioTest` Gradle task가 있다.
- [ ] CI에서 `scenario-test` job이 별도 실행된다.
- [ ] 대표 사용자 흐름 테스트가 최소 1개 있다.
- [ ] 시나리오 추가 기준이 문서화된다.
- [ ] `./gradlew check scenarioTest`가 통과한다.
