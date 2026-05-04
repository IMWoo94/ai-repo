# ADR-0021: Release Version Baseline

## 상태

Accepted

## 맥락

현재 `main`에는 잔액/거래내역, 회원/지갑, 충전/송금, 원장/감사 로그, PostgreSQL/Flyway, outbox relay, manual review, requeue 감사 이력까지 누적되어 있다.

하지만 Git tag와 GitHub Release가 없으면 어떤 커밋이 검증 가능한 기준선인지 설명하기 어렵다. 포트폴리오형 핀테크 학습 프로젝트에서는 기능 구현뿐 아니라 릴리스 단위, 검증 결과, 알려진 리스크를 함께 남겨야 한다.

## 선택지

### 선택지 A: tag 없이 `main` 최신 커밋만 기준으로 삼는다

장점:

- 별도 릴리스 관리 비용이 없다.
- 개발 속도가 빠르다.

단점:

- 검증된 기준선을 재현하기 어렵다.
- 문서, 테스트 결과, 알려진 리스크가 특정 커밋에 묶이지 않는다.
- 포트폴리오 검토자가 어느 상태를 봐야 하는지 불명확하다.

### 선택지 B: Git tag와 GitHub Release로 검증 기준선을 남긴다

장점:

- 검증된 기능 묶음을 재현 가능한 버전으로 고정할 수 있다.
- 릴리스 노트에 기능, 마이그레이션, 테스트 결과, 리스크를 함께 남길 수 있다.
- 이후 broker, 인증/인가, 승인 워크플로우 같은 큰 변경의 기준점이 된다.

단점:

- 릴리스 노트와 버전 갱신 비용이 생긴다.
- 실제 운영 배포가 아니므로 릴리스 의미를 과장하지 않도록 범위를 명확히 해야 한다.

### 선택지 C: Gradle version만 갱신하고 GitHub Release는 만들지 않는다

장점:

- 애플리케이션 버전은 코드에 남는다.
- GitHub Release 작성 비용은 없다.

단점:

- GitHub에서 검증 기준선을 직접 확인하기 어렵다.
- 테스트 결과와 알려진 리스크가 버전에 연결되지 않는다.

## 결정

`Git tag와 GitHub Release로 검증 기준선을 남긴다`를 선택한다.

구현 기준은 다음과 같다.

| 항목 | 결정 |
| --- | --- |
| 첫 릴리스 버전 | `v0.6.0` |
| Gradle version | `0.6.0` |
| Release note | `docs/releases/v0.6.0.md` |
| Release 생성 시점 | PR merge 후 `main` 최신 커밋 |
| 검증 기준 | `./gradlew check`, `docker compose config`, GitHub CI |
| 범위 제외 | 실제 배포 자동화, 컨테이너 이미지 publish, broker 연동 |

## 결과

장점:

- 현재 학습 기준선을 `v0.6.0`으로 재현할 수 있다.
- release note가 README보다 자세한 검증 증거가 된다.
- 이후 대규모 변경 전 안정 기준점이 생긴다.

비용:

- 릴리스마다 문서와 버전을 함께 갱신해야 한다.
- 실제 운영 배포가 아니므로 “검증 기준선”과 “운영 배포”를 구분해야 한다.

후속 작업:

- `v0.8.0` 이후 후보로 broker-specific adapter와 consumer idempotency를 설계한다.
- GitHub Wiki 동기화 절차를 릴리스 체크리스트에 연결한다.
- 필요하면 컨테이너 이미지 빌드/검증 단계를 추가한다.
