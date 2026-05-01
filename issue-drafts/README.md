# Issue Drafts

이 폴더는 GitHub Issue로 옮기기 전 작업 초안을 보관합니다.

초안은 `.github/ISSUE_TEMPLATE/feature.yml` 또는 `.github/ISSUE_TEMPLATE/bug.yml`에 맞춰 작성합니다. GitHub Issue를 생성한 뒤에는 실제 Issue 링크를 PR과 Wiki에 연결합니다.

## 목록

- `0001-balance-and-transaction-history.md`: 첫 기능 흐름인 잔액/거래내역 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/1
- `0002-member-wallet-account.md`: 회원/지갑 계정 모델 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/3
- `0003-charge-and-transfer.md`: 충전/송금 1차 흐름 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/5
- `0004-ledger-and-audit-log.md`: 원장/감사 로그 1차 모델과 조회 API 작업 초안
  - GitHub Issue: https://github.com/IMWoo94/ai-repo/issues/7

## GitHub CLI로 생성

GitHub CLI가 설치되어 있고 인증되어 있다면 다음 명령으로 첫 Issue를 생성할 수 있습니다.

```bash
gh issue create \
  --repo IMWoo94/ai-repo \
  --title "[Feature] 잔액 조회와 거래내역 조회 기준 정의" \
  --label feature \
  --body-file issue-drafts/0001-balance-and-transaction-history.md
```
