# [Feature] Operator Manual Review Console UI

## 작업 요약

운영자 화면에 manual review outbox 조회, requeue 실행, requeue audit 확인 UI를 추가하고 Apple 스타일 기준으로 card, status badge, empty/error state를 정리한다.

## 배경과 문제

- 백엔드에는 manual review, requeue, requeue audit 운영 API가 있다.
- 현재 사용자 화면은 일반 지갑 흐름 중심이라 운영자가 장애 outbox를 화면에서 검증하기 어렵다.
- 포트폴리오형 운영 하네스에서는 백엔드 운영 API를 사람이 확인 가능한 콘솔로 노출해야 한다.

## 범위

### 하는 것

- 운영자 token/operator 입력 UI를 추가한다.
- manual review outbox 목록을 조회한다.
- 선택한 outbox event를 reason과 함께 requeue한다.
- 선택한 outbox event의 requeue audit 목록을 조회한다.
- Apple 스타일 기준으로 card, status badge, empty/error state를 정리한다.
- 관련 프론트 테스트와 문서를 갱신한다.

### 하지 않는 것

- 운영자 로그인/OIDC/IAM은 도입하지 않는다.
- 승인 워크플로우는 이번 범위에 포함하지 않는다.
- 백엔드 API 계약은 변경하지 않는다.

## 수용 기준

- [x] 화면에서 운영자 token/operator를 입력할 수 있다.
- [x] manual review outbox 목록을 조회할 수 있다.
- [x] manual review event가 없으면 empty state를 보여준다.
- [x] requeue reason 입력 후 requeue를 실행할 수 있다.
- [x] 선택한 outbox event의 requeue audit을 조회할 수 있다.
- [x] 401/403/API 오류를 error state로 표시한다.
- [x] Apple 스타일 기준의 card/status badge/empty/error state가 적용된다.
- [x] 프론트 unit test와 build가 통과한다.

## 도메인 규칙과 불변식

- requeue는 운영자 식별자와 reason을 필요로 한다.
- requeue audit은 운영자 행위의 증거이므로 화면에서 확인 가능해야 한다.
- manual review event가 없다는 상태는 실패가 아니라 정상 empty state다.

## 하네스 역할 체크

- 기획자: 운영자가 장애 outbox를 화면에서 확인하고 조치할 수 있어야 한다.
- 도메인 전문가: requeue는 감사 이력과 함께 검증되어야 한다.
- 코드 개발자 A: 기존 API 계약을 유지하고 프론트 상태만 확장한다.
- 코드 개발자 B: 일반 사용자 지갑 흐름과 운영자 콘솔 상태가 과도하게 섞이지 않도록 한다.
- QA: empty/error/requeue success/audit 조회를 테스트한다.
- 릴리스 관리자: 로컬 테스트와 프론트 실행 문서를 갱신한다.

## 검증

```bash
cd frontend
npm run test
npm run build
npm run e2e
```

## 관련 Issue

- GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/77
