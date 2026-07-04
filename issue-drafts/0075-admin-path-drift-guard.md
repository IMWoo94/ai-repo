# Admin Path Drift Guard

## 배경

운영 API 경로 목록이 `SecurityConfig`(인증 chain)와 `AdminApiPathMatcher`(헤더 인증·접근 감사 필터)에 별도 상수로 존재한다. 한쪽만 갱신하면 drift(#109 계열)가 재발할 수 있다.

## 목표

- 두 목록의 일치(또는 단일 source 참조)를 검증하는 테스트를 추가한다.
- 불일치 시 실패 메시지가 누락 경로를 명시한다.

## 완료 조건

- [x] 두 목록이 같은 운영 API root 집합을 다루는지 양방향으로 검증하는 테스트를 추가한다.
- [x] 각 운영 pattern root의 root/sub-path는 matcher가 true, lookalike prefix는 false임을 고정한다.
- [x] 불일치 시 실패 메시지가 누락 경로를 명시한다.

## 검증 명령

```bash
./gradlew test --tests '*OperationalApiPathDriftGuardTest' --tests '*AdminApiPathMatcherTest'
scripts/check-dev-rules.sh
```
