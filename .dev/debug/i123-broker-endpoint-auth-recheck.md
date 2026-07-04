# i123 broker endpoint auth — 재검증 트러블슈팅 메모

- 대상 이슈: #123 (broker 엔드포인트 인증), 후속 #134 (디버그 잔여물 제거·지연 원인 기록)
- 브랜치: `feat/broker-endpoint-auth` (origin/main에서 분기, 변경 전부 uncommitted)
- GitHub 기록: https://github.com/IMWoo94/ai-repo/issues/134#issuecomment-4881455462

## 결론
초기 분석의 주요 원인 판단은 정합적이다. 단 "DIAG println이 지연의 직접 증거"라는 한 문장만 저장소로는 입증 불가라 톤다운이 필요하다.

## 확인된 것 (green)
- 인증 순서/계약 문제 진단 정확:
  - `src/main/java/com/imwoo/airepo/wallet/api/SecurityConfig.java` — `/internal/broker/**` 전용 `@Order(3)` `brokerSecurityFilterChain` 추가.
  - `src/main/java/com/imwoo/airepo/wallet/api/BrokerTokenAuthenticationFilter.java` — `X-Broker-Token` 없으면 401.
- publisher 계약 동반 수정:
  - `src/main/java/com/imwoo/airepo/wallet/infra/HttpOperationOutboxPublisher.java` — `X-Broker-Token` 헤더 부착.
  - `src/main/resources/application.yml` — consumer/publisher 양쪽 `broker-token` 기본값 `local-broker-token` 일치.
- monitoring/alert 컨트롤러 테스트 토큰 헤더 1줄 추가 정당(helper가 `/internal/broker/outbox-events` 직접 호출).
- 잔여물 재스캔: `System.out`/`System.err`/`printStackTrace`/`DIAG` 워킹트리 0건.
- 테스트 재실행 통과 (fail/skip/error 0):
  | 클래스 | tests |
  |---|---|
  | BrokerTokenAuthenticationFilterTest | 4 |
  | OperationOutboxConsumerControllerTest | 4 |
  | OperationOutboxConsumerMonitoringControllerTest | 3 |
  | OperationalAlertControllerTest | 4 |
  | HttpOperationOutboxPublisherContractTest | 3 |
  | HttpOutboxPublishConsumeLoopTest | 1 |

## 입증 불가 (톤다운 대상)
- "DIAG println이 오래 걸린 직접 증거": `git log -S 'DIAG' --all` 0건, reflog/stash 0건. 중간 커밋이 없어 과거 존재 여부조차 repo로는 재구성 불가 → 세션 transcript로만 확정 가능.
- "락 이슈 아님" 결론은 코드로 재확인됨(무상태 토큰 비교, relay 기존 claim-lease 유지).

## 남은 액션
- #134 본문 "정확한 원인" 문구를 세션 로그 근거의 추정 원인으로 톤다운 또는 transcript 링크 첨부.
- #123 PR 반영 시 위 테스트 green + 잔여물 0건 상태 유지.
