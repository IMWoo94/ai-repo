---
name: spec-generator
description: >
  Use when structured requirements or deep-interview results need to be
  transformed into a complete specification document. Generates an 8-section
  spec covering background, users, scenarios, data/state, integrations,
  permissions, scope/success criteria, and development policy.
---

# Spec Generator

Use this skill to convert clarified requirements into a formal spec document.

## Inputs

Accept any of these:

- Deep-interview final alignment.
- Existing requirements notes.
- An Issue draft.
- A repository planning conversation.

If the user specifies an output folder, write the spec there. Otherwise use `specs/{feature-slug}-spec.md`.

## Required Sections

Generate the spec with these sections:

1. 배경
2. 사용자와 목표
3. 사용 시나리오
4. 데이터와 상태
5. 연결되는 시스템
6. 권한
7. 범위와 성공 기준
8. 개발 정책

Add `미결 사항` when decisions are intentionally deferred.

## Writing Rules

- Use clear Korean suitable for product, domain, QA, and engineering review.
- Keep completion criteria testable.
- Do not hide uncertainty.
- Mark assumptions as `[추정]` if the user did not explicitly confirm them.
- Mark repository-derived facts as `[Context 기반]` only when directly grounded in files or prior accepted documents.
- High-risk fintech policy must not be silently guessed.
- Separate "이번에 하는 것" and "이번에 안 하는 것".
- Include tradeoffs when a decision affects architecture, process, testing, or release.

## Output Shape

```markdown
# {기능명 또는 주제} — 기획서

## 1. 배경

## 2. 사용자와 목표
| 사용자 유형 | 목표 | 주요 행동 |
| --- | --- | --- |

## 3. 사용 시나리오

### 정상 흐름

### 예외 상황
| 상황 | 사용자에게 보이는 것 | 시스템 동작 |
| --- | --- | --- |

## 4. 데이터와 상태

### 주요 데이터
| 항목 | 설명 | 필수 여부 | 제약 |
| --- | --- | --- | --- |

### 상태 변화
| 현재 상태 | 이벤트 | 다음 상태 | 부수 효과 |
| --- | --- | --- | --- |

## 5. 연결되는 시스템
| 시스템 | 연동 내용 | 방향 |
| --- | --- | --- |

## 6. 권한
| 역할 | 할 수 있는 것 | 할 수 없는 것 |
| --- | --- | --- |

## 7. 범위와 성공 기준

### 이번에 하는 것

### 이번에 안 하는 것

### 성공 기준
| 기준 | 완료 판단 |
| --- | --- |

## 8. 개발 정책
| 결정 항목 | 정책 | 근거 |
| --- | --- | --- |

## 미결 사항
```

## Project Notes for ai-repo

For this repository:

- Write project-level specs under a topic-specific folder when requested.
- Preserve the agreement that ADR is the source of truth for decisions.
- Preserve the agreement that Wiki stores long-form role thinking and process logs.
- Include harness roles and review criteria in specs when the topic is process or governance.
- Include document quality, code+test completion, release notes, and execution verification in success criteria.
- Keep deferred decisions explicit, especially Spring Boot version, Gradle version, MCP adoption, deployment target, and security depth.
