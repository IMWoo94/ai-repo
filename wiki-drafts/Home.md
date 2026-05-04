# Home

이 Wiki는 Java 25/Spring Boot 기반 핀테크 학습 프로젝트의 1차 MVP 진행 상태를 설명한다.

결정의 source of truth는 `docs/adr/`이고, 작업 완료 흔적은 `docs/progress/`이다. Wiki는 포트폴리오형 설명, 긴 사고 과정, 역할별 운영 기준을 읽기 쉽게 정리하는 공간이다.

## 현재 릴리스 상태

- 현재 릴리스 후보: `v0.7.0`
- 기준 문서: `docs/releases/v0.7.0.md`
- Wiki publish 상태: `v0.7.0` 기준 publish 완료
- 로컬 smoke 명령: `scripts/mvp-local-smoke.sh`
- 핵심 실행 화면: React 사용자 화면과 운영자 manual review 콘솔

## 1차 MVP 범위

- 잔액과 거래내역 조회
- 회원과 지갑 계정
- 충전과 송금
- 원장과 감사 로그
- PostgreSQL profile, Flyway, Testcontainers 검증
- Operation step log
- Transactional outbox와 relay 상태 전이
- Manual review, requeue, requeue audit
- 운영 API 인증/인가와 접근 감사
- React 사용자 화면과 운영자 콘솔
- backend, frontend, PostgreSQL scenario CI gate

## Wiki 지도

| 문서 | 목적 |
| --- | --- |
| `Domain-Rules` | 도메인 불변식, 돈 이동, outbox, 운영자 조치 규칙 |
| `Architecture-Decisions` | ADR을 읽기 쉬운 결정 지도로 요약 |
| `Development-Workflow` | 브랜치, PR, 리뷰, 테스트, 릴리스 흐름 |
| `Harness-Roles` | 기획자, 도메인 전문가, 개발자, QA, 릴리스 관리자 책임 |
| `QA-Scenarios` | MVP 시연과 회귀 검증 시나리오 |
| `Release-Notes` | 현재 release candidate와 출시 전 blocker |
| `MCP-and-Skills` | MCP/스킬 사용 원칙과 후보 |

## 출시 전 확인

릴리스 tag 발행 전에는 다음을 확인한다.

- GitHub Actions 전체 gate 통과
- `scripts/mvp-local-smoke.sh` 통과
- `docs/releases/v0.7.0.md` release note 확정
- Wiki draft를 GitHub Wiki checkout에 동기화
- 릴리스 PR에 smoke 결과와 알려진 제약을 첨부
