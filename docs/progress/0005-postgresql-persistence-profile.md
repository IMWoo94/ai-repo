# 0005. PostgreSQL 저장소 프로필

## 스펙 목표

- 인메모리 저장소만으로는 금융 정합성 검증이 부족하므로 PostgreSQL 저장소를 추가한다.
- 기본 실행은 빠른 인메모리 저장소를 유지하고, `postgres` 프로필에서 실제 DB 저장소를 사용한다.
- H2 기반 테스트와 PostgreSQL 운영 유사성 검증을 병행한다.

## 완료 결과

- `JdbcWalletRepository`를 추가했다.
- `application-postgres.yml`과 PostgreSQL 프로필을 추가했다.
- PostgreSQL용 스키마와 fixture SQL을 추가했다.
- 금액 타입과 저장소 계층 테스트를 보강했다.

## 검증

- JDBC repository 테스트로 충전/송금 결과 저장과 조회를 검증했다.
- H2 테스트 스키마로 빠른 repository 검증을 유지했다.

## 남은 일

- 운영급 DB migration 관리는 이 단계에서는 아직 완성하지 않았다.
- 실제 PostgreSQL 컨테이너 기동 검증은 다음 단계로 분리했다.

## 관련 문서

- `docs/adr/0008-postgresql-persistence-profile.md`
- `issue-drafts/0005-postgresql-persistence.md`
- `src/main/java/com/imwoo/airepo/wallet/infra/JdbcWalletRepository.java`
