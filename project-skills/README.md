# Project Skills

이 폴더는 `ai-repo` 작업에서 실제 사용한 Codex 스킬의 프로젝트용 스냅샷입니다.

개인 환경의 `~/.codex/skills`에 있는 스킬을 그대로 의존하지 않고, 이 레포의 하네스 엔지니어링 흐름을 재현할 수 있도록 필요한 `SKILL.md`를 보관합니다.

## 포함 스킬

- `deep-interview`: 요구사항과 운영 방향의 모호성을 0.2 이하로 낮추는 구조적 인터뷰
- `spec-generator`: 인터뷰 합의 내용을 8개 섹션 기획 스펙으로 변환

## 사용 원칙

- 이 폴더는 레포 기록용입니다.
- Codex 자동 발견을 원하면 각 스킬 폴더를 `$CODEX_HOME/skills` 또는 `~/.codex/skills`로 복사합니다.
- 프로젝트에서 수정한 스킬은 `scripts/sync-codex-skills.py`와 별개로 관리합니다.
