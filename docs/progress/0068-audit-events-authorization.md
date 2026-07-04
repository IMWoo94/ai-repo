# 0068. Audit Events and Operation Log Authorization

## 스펙 목표

- 미인증 노출된 `GET /api/v1/audit-events`, `/api/v1/operations/{id}/step-logs`, `/api/v1/operations/{id}/outbox-events`를 닫는다.
- 사용자가 자기 지갑의 감사 로그를 볼 수 있는 소유자 스코프 엔드포인트를 추가한다(결정: "둘 다").

## 완료 결과

- 세 엔드포인트를 `SecurityConfig.OPERATIONAL_API_PATHS`에 편입해 admin chain(GET operator role)으로 보호한다. 미인증은 `AdminSecurityErrorHandler`가 401을 반환한다.
- `AdminApiPathMatcher`에 `/api/v1/audit-events`, `/api/v1/operations` prefix를 추가해 헤더 인증 필터와 접근 감사 필터가 두 경로에도 적용되게 했다.
- `GET /api/v1/wallets/{walletId}/audit-events`를 wallet chain(JWT)에 추가했다. `WalletAccessPolicy.findOwnedQueryableWallet`로 소유권을 검증(비소유자 403 `WALLET_ACCESS_DENIED`)한 뒤, `audit_events WHERE operation_id IN (SELECT operation_id FROM ledger_entries WHERE wallet_id = ?)`를 occurredAt/auditEventId 역순으로 반환한다.
- 포트 `WalletLedgerQueryRepository.findAuditEventsByWallet`를 Jdbc(조인 쿼리)와 InMemory(ledger op 집합 필터) 어댑터 양쪽에 구현했다. 기존 파라미터 없는 `getAuditEvents()`는 operator 컨트롤러 경로에서만 사용한다.
- 프론트엔드 유저 화면은 소유자 스코프 `/api/v1/wallets/{walletId}/audit-events`를 호출하고, operator 전용 step-logs/outbox-events 호출을 유저 evidence에서 제거했다.
- 회귀 테스트: 세 엔드포인트 미인증 401, operator 헤더 200, 소유자 스코프 감사 로그 소유자 성공/비소유자 403/미인증 401, 다른 지갑 audit 미포함.

## 리뷰 라운드 (multi-agent 보안 리뷰, 10건 제기 → 5건 확정)

- **transfer 상대방 walletId 노출**: 소유자 스코프 조회가 operation_id 조인이라 transfer 감사 이벤트(detail에 양쪽 walletId)가 송신자·수신자 모두에게 반환됨. **수용 결정** — transfer 응답이 이미 `counterpartyWalletId`를 반환하고, walletId는 소유권 검증 때문에 권한을 부여하지 않음. ADR-0058 트레이드오프에 문서화 + `returnsTransferAuditEventWithCounterpartyToBothWalletOwners` 회귀 테스트로 고정.
- **JDBC 경로 미검증**: `JdbcWalletRepository.findAuditEventsByWallet` 테스트 0건 → `PostgresContainerWalletRepositoryTest`에 두 지갑 교차 배제 테스트 추가(Docker 필요, `postgresScenarioTest` 태스크).
- **HTTP 레이어 교차 배제 미검증**: `returnsWalletAuditEventsForOwner`에 wallet-002 충전 추가 후 op-002 배제 단언.
- **프론트 mock 회귀 감지 불가**: `endsWith('/audit-events')`가 구 operator 엔드포인트와도 매치 → 전체 wallet 스코프 URL + Bearer 헤더 단언 추가.
- **빈 패널 잔존**: 유저 화면의 Step Log/Outbox `EvidencePanel` 제거, 고아가 된 `stepLogs`/`outboxEvents` 상태와 `OperationStepLog` 타입 제거(`OperationOutboxEvent`는 operator manual-review에서 사용 중이라 유지).
- **e2e 후속(main 머지 후 CI 발견)**: `wallet-flow.spec.ts`의 step log/outbox 단언을 원장(`CREDIT · 잔액`)·감사 로그(`CHARGE_COMPLETED`, transfer 상대방 detail 포함) 단언으로 교체.

## 개선 건수

1. audit-events/step-logs/outbox-events 미인증 bulk 노출 차단(operator 게이팅).
2. 소유자 스코프 `/wallets/{walletId}/audit-events`로 사용자 감사 가시성 유지.

## 검증

- `./gradlew test` (274 통과, 1 skipped)
- `npm --prefix frontend run test` (10 통과)
- `npm --prefix frontend run build`의 `tsc --noEmit` 타입 체크 통과

## 남은 일

- `audit_events` wallet_id 비정규화로 ledger 조인 제거(현재는 조인 의존)
- 소유자 스코프 step-logs/outbox-events가 필요하면 별도 후속 검토
- Docker 환경에서 `./gradlew postgresScenarioTest` 실행으로 신규 JDBC 테스트 검증(작성 시점 Docker 미가동으로 컴파일만 확인)

## 관련 문서

- `docs/adr/0058-audit-events-authorization.md`
- `docs/superpowers/specs/2026-07-04-audit-events-authz-design.md`
- `docs/releases/unreleased.md`
