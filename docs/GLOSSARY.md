# Glossary (용어집)

> ai-repo 지갑-원장 도메인과 아웃박스/운영 파이프라인에서 쓰는 용어를 정의합니다. 상세 규칙은 [Domain-Rules](../wiki-drafts/Domain-Rules.md), 구조는 [Architecture Overview](ARCHITECTURE.md)를 참고하세요.

## 도메인 (지갑·원장)

| 용어 | 정의 |
| --- | --- |
| **회원 (Member)** | 지갑을 소유하는 주체. 엔드유저 JWT의 subject(`memberId`)와 매핑된다. `active` 상태만 인증/거래 가능. |
| **지갑 계정 (Wallet Account)** | 회원이 소유한 계정. 하나의 `WalletBalance`를 가진다. `WalletAccount` 레코드. |
| **잔액 (Balance)** | 지갑의 현재 금액. `WalletBalance` = `Money`(BigDecimal 금액 + `KRW` 통화). 음수 불가. |
| **Money** | 금액 값객체. `BigDecimal amount + Currency(KRW 단일)`. 통화 불일치·음수는 도메인 불변식으로 거부. |
| **거래 내역 (Transaction History)** | 사용자에게 보여주는 거래 항목(`TransactionHistoryItem`). 조회 관점. 현재 모두 `COMPLETED` 상태만 존재. |
| **원장 항목 (Ledger Entry)** | 정합성/무결성 관점의 기록(`LedgerEntry`). `balanceAfter`를 담아 잔액 추적 가능. 감사·조회용 거래 내역과 **의도적으로 분리**된 별개 레코드. |
| **감사 이벤트 (Audit Event)** | 명령(command) 관점의 감사 기록(`AuditEvent`). 누가/언제 어떤 명령을 실행했는지. |
| **충전 (Charge)** | 지갑 잔액을 늘리는 명령. 현재는 **단측(single-sided)** — 상대(house) 계정 없이 잔액 증가. |
| **송금 (Transfer)** | 두 지갑 간 잔액 이동. 출금측/입금측 **양측(two-sided)** 기록. |
| **정산 (Settlement)** | (미구현/후속) 외부 정산·house 계정 대사. 진짜 복식부기로 가려면 필요. |

## 멱등성·동시성

| 용어 | 정의 |
| --- | --- |
| **멱등키 (Idempotency-Key)** | 충전/송금 요청의 중복 방지 키(필수). 같은 키+같은 payload → 이전 결과 재생(replay). 같은 키+다른 payload → `409 IDEMPOTENCY_KEY_CONFLICT`. (ADR-0006) |
| **fingerprint** | 요청 payload의 해시. 멱등키 재사용이 동일 요청인지 판별하는 데 사용. |
| **행 잠금 (Row Locking)** | `postgres` 프로필에서 `SELECT .. FOR UPDATE`로 잔액 행을 잠금. 두 지갑은 **결정적 순서**로 잠가 데드락 회피. (ADR-0011) |
| **lock_timeout** | 잠금 대기 상한(`SET LOCAL`). 초과 시 `WALLET_BALANCE_BUSY`(409) 반환. (ADR-0012) |

## 아웃박스 파이프라인

| 용어 | 정의 |
| --- | --- |
| **트랜잭셔널 아웃박스 (Transactional Outbox)** | 상태 변경과 같은 트랜잭션에 이벤트 행(`PENDING`)을 함께 기록해, 발행 의도를 원자적으로 커밋하는 패턴. (ADR-0014) |
| **아웃박스 이벤트 (Outbox Event)** | 발행 대상 이벤트(`OperationOutboxEvent`). 상태: `PENDING → SENT` 또는 `MANUAL_REVIEW`. 리스/시도횟수 등 불변식 보유. |
| **릴레이 (Relay)** | `PENDING` 이벤트를 claim해 브로커로 발행하는 배경 작업(`OperationOutboxRelayService`). `FOR UPDATE SKIP LOCKED`, 60초 리스, 30초 백오프, 최대 3회. (ADR-0015~0018) |
| **리스 (Lease)** | 릴레이가 이벤트를 점유하는 시간 창(60초). 만료 시 다른 릴레이가 회수 가능 → 중복/유실 없이 재처리. (ADR-0017) |
| **수동 검토 (Manual Review)** | 최대 시도 소진된 이벤트가 가는 상태. 운영자 개입 대상. (ADR-0018) |
| **4-eyes / requeue 승인 워크플로우** | 수동 검토 이벤트 재투입 절차: `REQUESTED → APPROVED → EXECUTED`. **승인자·반려자는 요청자와 달라야 함**. 직접 requeue API는 폐기(`410 Gone`). (ADR-0038/0040) |
| **컨슈머 (Consumer)** | 발행된 이벤트를 수신·처리(`POST /internal/broker/outbox-events`). `X-Broker-Token` shared secret 인증 필요(미인증 401). 멱등 dedup, receipt/delivery metric 기록. (ADR-0043~0046, 0065) |
| **receipt** | 컨슈머가 이벤트를 처리했다는 영수증 기록. 재처리 판별·모니터링에 사용. |
| **스텝로그 (Step Log)** | 하나의 명령이 거친 단계 감사기록(`OperationStepLog`, `BALANCE_LOCKED..IDEMPOTENCY_RECORDED`). tx 내 감사 목적이며 **보상 saga가 아님**. (ADR-0013) |

## 운영·시큐리티

| 용어 | 정의 |
| --- | --- |
| **operator / admin 토큰** | 운영 API 접근 헤더 토큰. 조회는 operator, 변경(requeue 승인·pruning)은 admin. 상수시간 비교. (ADR-0037) |
| **broker 토큰 / X-Broker-Token** | broker→consumer shared secret 헤더. `/internal/broker/**` 전용 Order 3 체인이 상수시간 비교로 검증(미인증 401), publisher가 같은 값 부착. 기본 토큰은 `BrokerTokenGuard`가 배포 프로파일(postgres/prod)에서 차단. (ADR-0065) |
| **operational alert** | relay/consumer health 경보 레코드. 같은 source/severity/reasons는 기본 15분 suppression. Slack webhook 또는 Noop로 발행. (ADR-0052~0054) |
| **pruning** | 오래된 관측 로그(relay-run, access-audit, processed-event, receipt, delivery metric)를 보존기간 후 정리. 스케줄러는 기본 비활성. (ADR-0032/0048/0051/0053) |
| **엔드유저 JWT** | 회원 로그인 토큰(HS256, sub=memberId). Bearer로 지갑 API 호출, 서비스 계층 소유권 검사. (ADR-0056/0057) |
| **ownership check** | 토큰 소유자와 지갑 소유자를 대조하는 per-user 접근 정책(`WalletAccessPolicy`). |
| **admin API access audit** | 운영 API 접근 성공/실패 이력 기록. (ADR-0031) |

## 하네스·프로세스

| 용어 | 정의 |
| --- | --- |
| **하네스 엔지니어링 (Harness Engineering)** | 역할별(기획/도메인/개발/QA/릴리스) 관점을 분리해 같은 기능을 반복 검증하는 AI-에이전트 주도 개발 방식. |
| **ADR** | Architecture Decision Record. 결정의 single source of truth. → [ADR Index](adr/README.md) |
| **Progress Report** | 각 작업의 완료 결과·검증·잔여를 짧게 남기는 기록. → [progress](progress/README.md) |
| **sync gate (dev-rules)** | 코드/DB/프론트/스크립트 변경 시 ADR·progress·wiki·test·release 문서 동기화 누락을 파일 변경 기준으로 검사하는 CI 잡(`scripts/check-dev-rules.sh`). (ADR-0039) |
