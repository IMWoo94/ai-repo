---
name: deep-interview
description: >
  Use when the user wants thorough alignment before building and a quick
  discussion or single clarification pass is not enough. Use for exposing
  hidden assumptions, removing ambiguity, and syncing on what to build before
  implementation. Triggers include "deep interview", "딥 인터뷰", "모호성 제거",
  "확실히 정리하자", and "끝까지 파고들자".
---

# Deep Interview

Use this skill to align on goals, constraints, success criteria, and hidden assumptions before creating specs or implementation plans.

## Hard Rules

- Do not write code.
- Do not create implementation tasks before the interview is closed.
- Ask one question per round.
- Minimum interview depth is 3 rounds.
- After round 3, score ambiguity after every round.
- Continue until ambiguity score is `≤ 0.2`, unless the user explicitly asks to go deeper.
- Treat user-provided existing repository context as Brownfield even if the codebase is still sparse.

## Phase 0: Seed Capture

Before asking the first question:

1. Capture the user's seed request without solving it.
2. Inspect repository context lightly when the request references a codebase.
3. Classify the work as `Greenfield` or `Brownfield`.
4. List implied assumptions and unknowns.
5. Stop and wait for the user to say they are ready.

Output format:

```markdown
## Seed Capture

**주제:** ...
**유형:** Greenfield / Brownfield
**내포된 가정:**
  - ...
**아직 모르는 것:**
  - ...

Round 1을 시작합니다.
```

## Phase 1: Interview Loop

Ask one focused question at a time.

Question rules:

- Target the weakest clarity area.
- Provide 2-4 mutually exclusive options.
- Allow the user to answer with a custom option.
- Prefer questions that change the eventual artifact or workflow.
- Do not ask generic questions that repository context already answers.

Round focus:

- Rounds 1-2: preserve breadth and expose assumptions.
- Rounds 3-5: add architecture, constraints, and success criteria.
- Rounds 6+: close remaining ambiguity or ask whether to continue.
- Brownfield: always include compatibility with the existing repository.

## Phase 2: Ambiguity Check

Starting after round 3, score the interview using Brownfield criteria when the work touches an existing repository.

Brownfield scoring:

```text
Ambiguity Score = 1.0 - (
  Goal Clarity × 0.35 +
  Constraint Clarity × 0.25 +
  Success Criteria × 0.25 +
  Context Clarity × 0.15
)
```

Output format:

```markdown
📊 Ambiguity Check — Round N
  Goal Clarity:        0.XX  (×35%)
  Constraint Clarity:  0.XX  (×25%)
  Success Criteria:    0.XX  (×25%)
  Context Clarity:     0.XX  (×15%)
  ─────────────────────────────────
  Ambiguity Score:     0.XX  [목표: ≤ 0.2]
  Status: 🔴 계속 필요 / 🟡 거의 도달 / 🟢 종료 가능

  가장 약한 영역: ...

📋 DECIDE_LATER:
  - ...
```

DECIDE_LATER may contain implementation details, external dependencies, or decisions that need a prototype. Do not use it to hide unclear goals or unclear success criteria.

## Phase 3: Close

When ambiguity score is `≤ 0.2`:

1. Summarize the final alignment.
2. List DECIDE_LATER separately.
3. Ask the user to choose the next step.

Next-step options:

- `/spec-generator`: create a formal spec from the agreement.
- `/execute`: start implementing agreed artifacts.
- `계속`: continue narrowing a specific area.
- `Done`: stop after the interview.

## Project Notes for ai-repo

For this repository, use these known alignment points unless the user changes them:

- Primary goal: `운영 하네스 + 포트폴리오형`.
- Documentation quality is a first-class completion criterion.
- Important decisions use ADR as source of truth.
- Long-form thought process and role logs go to Wiki.
- First domain flow: `잔액/거래내역 → 회원/지갑 계정 → 충전/송금`.
- Completion includes documents, code, tests, release notes, and at least local execution verification when releasable.
- Harness strength starts detailed and becomes checklist-based after patterns stabilize.
