# 0012. Outbox Relay 상태

## 스펙 목표

- 저장된 outbox event가 발행 대기, 발행 성공, 발행 실패 중 어느 상태인지 추적한다.
- 발행 실패 횟수와 마지막 오류를 남긴다.
- 실제 broker adapter를 붙이기 전 relay가 사용할 최소 상태 전이 경계를 만든다.

## 완료 결과

- outbox event에 `attemptCount`, `publishedAt`, `lastError`를 추가했다.
- pending event 조회, 발행 성공 표시, 발행 실패 표시를 담당하는 relay 서비스를 추가했다.
- Flyway V5 migration으로 relay 상태 컬럼을 추가했다.
- in-memory와 JDBC repository 모두 relay 상태 전이를 지원하도록 했다.

## 검증

- `./gradlew test --rerun-tasks`가 통과했다.
- `./gradlew check`가 통과했다.
- GitHub Actions `Gradle Check`가 통과했다.

## 남은 일

- `SKIP LOCKED` 기반 claiming은 아직 없다.
- retry backoff와 재처리 스케줄은 아직 없다.
- 실제 broker adapter와 consumer idempotency key 정책은 후속 작업이다.

## 관련 문서

- `docs/adr/0015-outbox-relay-state.md`
- `issue-drafts/0012-outbox-relay-state.md`
- `src/main/java/com/imwoo/airepo/wallet/application/OperationOutboxRelayService.java`
- `src/main/resources/db/migration/V5__add_outbox_relay_state.sql`
