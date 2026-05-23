# Apple-Style UI Design Guide

## Purpose

Use this guide when creating or changing user-facing screens in this repository. It defines the local UI baseline for the React wallet and operator console so UI work stays consistent across feature branches.

## Principles

- Prefer a quiet, operational interface over marketing-style composition.
- Put the primary workflow on the first screen; avoid landing-page copy for tools.
- Keep layouts dense enough for repeated operator use, but leave clear spacing between unrelated workflows.
- Use direct labels for financial and operational state. Avoid decorative language in transaction, alert, and admin flows.
- Keep local filesystem details out of UI text, docs, and code comments.

## Layout

- Use full-width page bands with constrained inner content for major sections.
- Use cards only for repeated records, compact summaries, modals, or framed tools.
- Do not nest cards inside cards.
- Keep controls near the data they affect.
- Tables and record lists should preserve stable row height and predictable column alignment.
- Empty, loading, success, warning, and error states must not shift surrounding layout unexpectedly.

## Controls

- Use buttons for explicit commands and keep button text short.
- Use segmented controls for mutually exclusive modes.
- Use checkboxes or toggles for binary settings.
- Use numeric inputs, steppers, or sliders for numeric values.
- Use menus for option sets.
- Use badges for status values such as `OK`, `WARNING`, `CRITICAL`, `MANUAL_REVIEW`, and `EXECUTED`.

## Typography

- Do not scale font size with viewport width.
- Use compact headings inside dashboards, operator panels, cards, and tool surfaces.
- Reserve large display text for true hero surfaces, which this app should rarely need.
- Keep letter spacing at `0`.
- Ensure long identifiers, idempotency keys, wallet IDs, and operation IDs wrap or truncate without overlapping adjacent content.

## Color

- Avoid one-note palettes dominated by a single hue family.
- Use restrained neutrals for surfaces and reserve stronger color for status, risk, and action feedback.
- Distinguish success, warning, danger, neutral, and informational states without relying on color alone.
- Do not use decorative gradient orbs, bokeh blobs, or purely atmospheric backgrounds.

## Responsive Behavior

- Define stable dimensions for boards, toolbars, counters, status chips, and repeated record rows.
- On small screens, stack related controls vertically before text becomes cramped.
- Text inside buttons and cards must remain readable and must not overflow its container.
- Primary data should remain inspectable on mobile; avoid hiding critical financial or operational state behind decoration.

## Verification

- For frontend changes, run component tests and the frontend build.
- For workflow changes, run the relevant Playwright E2E test.
- When visual layout changes materially, inspect desktop and mobile widths before finishing.
