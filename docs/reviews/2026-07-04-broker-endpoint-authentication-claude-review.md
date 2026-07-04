# Broker Endpoint Authentication Claude Multi-Agent Review

> AGENTS.md 기본 흐름의 1차 리뷰는 Codex다. 이 문서는 사용자가 명시적으로 요청한(ultracode) Claude 멀티에이전트 커밋 전 리뷰 결과이며, Codex 1차 리뷰를 대체하지 않는다.

## 검토 대상

- 범위: `feat/broker-endpoint-auth` 워킹트리 diff (#123 완료)
- 변경 의도: 미인증 `POST /internal/broker/outbox-events`에 shared secret 헤더(`X-Broker-Token`) 인증을 추가하고 publisher가 같은 secret을 부착하도록 계약을 갱신한다.
- 방법: 4개 렌즈(auth-security / contract-config / test-coverage / issue-completeness) 병렬 리뷰 → 각 finding 적대적 재검증(refute-우선). 16개 에이전트, 0 에러.

## 검증

```bash
scripts/check-dev-rules.sh                          # PASS
./gradlew test scenarioTest postgresScenarioTest    # BUILD SUCCESSFUL
```

## 판정

통과. 블로커/HIGH 없음. MEDIUM 1건 수용(수정), LOW 3건 후속 과제.

## 리뷰 반영

### 수용

- **[MEDIUM] 기본 토큰 fail-open**: consumer 토큰이 env 미주입 시 커밋된 상수 `local-broker-token`으로 떨어져, ADR이 명시한 "2차 방어선"이 무력화된다. 형제 `JwtSecretGuard`가 배포 프로파일(postgres/prod)에서 커밋된 기본 secret에 startup fail-fast를 거는 선례가 있으나 broker token엔 없었다.
  - 조치: `BrokerTokenGuard`를 `JwtSecretGuard`와 동일 패턴으로 추가(DeployedProfiles = postgres/prod에서 기본값이면 startup 실패, 그 외 경고). 단위 테스트 5종 추가. ADR-0065 / progress 0074에 명시.

### 후속 과제

- **[LOW] 필터 path 가드 불일치**: `BrokerTokenAuthenticationFilter.isBrokerPath()`가 `startsWith("/internal/broker/")`(trailing slash)라, matcher `/internal/broker/**`가 매칭하는 slash-less `/internal/broker`는 토큰 검사 없이 통과한다. 현재 해당 base path에 매핑된 핸들러가 없어 404, 악용 불가. 방어적 하드닝으로 후속.
- **[LOW] error handler escape 불일치**: `BrokerSecurityErrorHandler`가 `AdminSecurityErrorHandler`와 달리 JSON escape 미적용. 메시지가 상수(`broker token is required`)라 현재 무해. 공통 writer로 통일 권장.
- **[LOW] 빈/공백 토큰 테스트 미커버**: `tokenMatches`의 `isBlank()` 분기 미검증. 단 `MessageDigest.isEqual` length mismatch로 사실상 무해(설정 토큰이 blank일 때만 유효). 회귀 고정용으로 후속.

### 반박 (재검증에서 INVALID)

- "shared secret이 두 config 키로 중복돼 drift 위험" → 코드 사실이나 이 워킹트리의 결함 아님(운영 설정 책임). 정보성.
- "필터가 `@Component`로 전역 재등록돼 이중 실행" → 실제 이중 실행/토큰 우회 없음. admin 필터와 동일 패턴.
- "401 JSON 인젝션" → 메시지가 항상 상수라 주입 벡터 없음.

## 관련

- 이슈 #123(기능), #134(디버그 잔여물/지연 원인 후속), ADR-0065, progress 0074.
