# ADR-0031: Admin API Access Audit

## 상태

Accepted

## 맥락

Outbox 운영 API는 `X-Admin-Token`과 `X-Operator-Id`로 보호된다. Requeue 행위와 relay scheduler 실행 결과도 별도 이력으로 남는다. 그러나 운영 API 자체를 누가 언제 호출했고, 성공했는지 실패했는지에 대한 접근 이력은 없었다.

금융/핀테크 운영에서는 데이터 변경뿐 아니라 조회도 감사 대상이다. Manual review 조회, relay run 조회, requeue audit 조회는 장애 대응과 정합성 확인에 사용되므로 접근 이력이 남아야 한다. 특히 인증 실패와 권한 실패도 기록되어야 운영 접근 오남용을 나중에 추적할 수 있다.

## 선택지

### 선택지 A: 애플리케이션 로그에만 기록한다

장점:

- 구현이 작고 DB 스키마 변경이 없다.
- 필터에서 로그만 남기면 기존 repository 경계가 바뀌지 않는다.

단점:

- 로컬/포트폴리오 환경에서 로그 보존과 검색 근거가 약하다.
- 테스트 코드로 접근 이력을 구조적으로 검증하기 어렵다.
- 운영 API로 최근 접근 이력을 조회할 수 없다.

### 선택지 B: 접근 감사 로그를 DB에 저장하고 API로 조회한다

장점:

- 성공/실패 접근 이력이 구조화된 데이터로 남는다.
- 인메모리와 PostgreSQL profile 모두 같은 정책을 테스트할 수 있다.
- 운영자가 최근 접근 이력을 API로 확인할 수 있다.

단점:

- 감사 로그 테이블과 repository가 추가된다.
- 보존 기간과 pruning 정책은 별도 결정이 필요하다.
- IP 주소, User-Agent 같은 보안 분석 필드는 아직 범위 밖이다.

### 선택지 C: Spring Security audit event로 바로 전환한다

장점:

- 인증/인가 이벤트와 principal 모델을 표준 보안 체계에 연결하기 좋다.
- Role 기반 권한과 함께 확장하기 좋다.

단점:

- 현재 프로젝트의 guard 기반 최소 인증/인가보다 도입 범위가 크다.
- Spring Security 전환 전에는 운영 API 접근 감사 요구를 빠르게 검증하기 어렵다.

## 결정

접근 감사 로그를 DB에 저장하고 API로 조회한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 이력 모델 | `AdminApiAccessAudit` |
| 결과 | `SUCCESS`, `FAILURE` |
| 저장 필드 | occurredAt, method, path, operatorId, statusCode, outcome |
| 제외 필드 | admin token |
| 기록 방식 | `OncePerRequestFilter` |
| 조회 API | `GET /api/v1/admin-api-access-audits?limit=50` |
| 권한 | `X-Admin-Token`, `X-Operator-Id` 필요 |
| PostgreSQL 테이블 | `admin_api_access_audits` |
| Flyway migration | `V10__create_admin_api_access_audits.sql` |

## 결과

장점:

- 운영 API 성공/실패 호출이 인메모리와 PostgreSQL 모두에 저장된다.
- 인증 실패도 status code와 함께 기록된다.
- 운영자는 최근 접근 감사 이력을 API로 조회할 수 있다.
- admin token은 저장하지 않아 민감정보 노출 가능성을 줄인다.

비용:

- 감사 로그 보존 기간과 삭제 배치는 아직 없다.
- IP 주소와 User-Agent는 아직 저장하지 않는다.
- Spring Security principal/role 모델과는 아직 연결되지 않았다.

후속 작업:

- 감사 로그 보존 기간과 pruning 정책을 추가한다.
- Spring Security 전환 시 principal, role, authority 정보를 감사 로그와 연결한다.
- 운영자 화면에서 접근 감사 이력을 조회할 수 있게 한다.
