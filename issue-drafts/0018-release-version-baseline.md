# [Release] v0.6.0 첫 검증 기준선 발행

GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/37

## 배경

현재 README는 릴리스 버전 관리 원칙을 정의하지만 실제 GitHub tag/Release는 아직 없다. 운영 하네스형 포트폴리오에서는 기능 완료뿐 아니라 어떤 기준선이 검증되어 배포 가능한지 추적할 수 있어야 한다.

## 목표

- 현재 `main` 기준 기능 묶음을 첫 GitHub Release 후보로 문서화한다.
- Gradle 프로젝트 버전과 릴리스 노트 기준을 맞춘다.
- 릴리스 전 검증 명령과 알려진 리스크를 기록한다.
- PR 병합 후 `v0.6.0` tag와 GitHub Release를 발행한다.

## 범위

- `build.gradle` 버전 갱신
- release note 문서 추가
- README, ADR, Wiki, Progress Report 갱신
- GitHub Release 발행 절차 기록

## 범위 제외

- 실제 운영 배포 자동화
- 컨테이너 이미지 빌드/푸시
- 브로커 연동
- 인증/인가 도입

## 수용 기준

- [ ] 릴리스 버전 정책 ADR이 있다.
- [ ] `build.gradle`의 버전이 릴리스 후보와 일치한다.
- [ ] `docs/releases/v0.6.0.md`에 포함 기능, 마이그레이션, 테스트 결과, 알려진 리스크, 다음 후보가 기록된다.
- [ ] `docs/progress`에 단계별 완료 흔적이 남는다.
- [ ] `./gradlew check`와 `docker compose config`가 통과한다.
- [ ] PR merge 후 `v0.6.0` tag와 GitHub Release가 생성된다.

## 하네스 역할 체크

- 기획자: 포트폴리오 기준선과 릴리스 설명이 이해 가능한지 확인한다.
- 도메인 전문가: 금융/핀테크 리스크와 범위 제외가 명확한지 확인한다.
- 개발자 A/B: 버전, 문서, 검증 명령의 일관성을 리뷰한다.
- QA: 릴리스 전 체크와 알려진 리스크가 재현 가능하게 기록됐는지 확인한다.
- 릴리스 관리자: tag/Release 발행 순서와 rollback 기준을 확인한다.
