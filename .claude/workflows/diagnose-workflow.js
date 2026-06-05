export const meta = {
  name: 'fintech-diagnosis',
  description: 'Diagnose main branch as a fintech-backend portfolio: parallel domain analysis, adversarial verification, prioritized backlog synthesis',
  phases: [
    { title: 'Analyze', detail: 'parallel domain analysts read main with a fintech-competence lens' },
    { title: 'Verify', detail: 'adversarially verify each high-severity finding against the actual code' },
    { title: 'Synthesize', detail: 'one diagnostician merges into a prioritized backlog + production-readiness view' },
  ],
}

// ---- Schemas -------------------------------------------------------------

const FINDINGS_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['area', 'strengths', 'findings'],
  properties: {
    area: { type: 'string' },
    strengths: {
      type: 'array',
      description: 'What this area does WELL from a fintech-backend competence standpoint. Be specific with file refs.',
      items: {
        type: 'object',
        additionalProperties: false,
        required: ['title', 'evidence'],
        properties: {
          title: { type: 'string' },
          evidence: { type: 'string', description: 'file:line refs and a concrete reason it is impressive/correct' },
        },
      },
    },
    findings: {
      type: 'array',
      description: 'Gaps, risks, bugs, or missing capabilities that would weaken a fintech-backend portfolio or block real operation.',
      items: {
        type: 'object',
        additionalProperties: false,
        required: ['title', 'severity', 'category', 'evidence', 'why_it_matters', 'suggested_fix'],
        properties: {
          title: { type: 'string' },
          severity: { type: 'string', enum: ['critical', 'high', 'medium', 'low'] },
          category: {
            type: 'string',
            enum: ['correctness', 'concurrency', 'data-integrity', 'security', 'observability', 'resilience', 'testing', 'architecture', 'operability', 'api-design', 'frontend', 'docs', 'performance'],
          },
          evidence: { type: 'string', description: 'concrete file:line refs proving the gap exists' },
          why_it_matters: { type: 'string', description: 'why a fintech reviewer / real operation would care' },
          suggested_fix: { type: 'string', description: 'concrete, surgical direction (not a rewrite)' },
        },
      },
    },
  },
}

const VERDICT_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['verdict', 'confidence', 'reasoning', 'corrected_severity'],
  properties: {
    verdict: { type: 'string', enum: ['confirmed', 'partially-confirmed', 'refuted', 'not-an-issue'] },
    confidence: { type: 'string', enum: ['high', 'medium', 'low'] },
    reasoning: { type: 'string', description: 'What you found when you actually read the code. Quote it. Default to refuting if you cannot prove the claim.' },
    corrected_severity: { type: 'string', enum: ['critical', 'high', 'medium', 'low', 'none'] },
  },
}

const SYNTHESIS_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['executive_summary', 'competence_verdict', 'top_backlog', 'production_gaps'],
  properties: {
    executive_summary: { type: 'string', description: '3-6 sentences: what this project is, how strong it is as a fintech-backend portfolio, and the single biggest lever to improve it.' },
    competence_verdict: {
      type: 'object',
      additionalProperties: false,
      required: ['rating', 'rationale', 'strongest_areas', 'weakest_areas'],
      properties: {
        rating: { type: 'string', enum: ['junior', 'mid', 'mid-to-senior', 'senior'], description: 'What seniority level this codebase credibly demonstrates for a fintech backend role.' },
        rationale: { type: 'string' },
        strongest_areas: { type: 'array', items: { type: 'string' } },
        weakest_areas: { type: 'array', items: { type: 'string' } },
      },
    },
    top_backlog: {
      type: 'array',
      description: 'Prioritized, de-duplicated improvement backlog. Each item is a candidate GitHub issue.',
      items: {
        type: 'object',
        additionalProperties: false,
        required: ['rank', 'title', 'severity', 'category', 'rationale', 'scope', 'effort'],
        properties: {
          rank: { type: 'integer' },
          title: { type: 'string', description: 'issue-style title, e.g. "feat: ..." / "fix: ..." / "test: ..."' },
          severity: { type: 'string', enum: ['critical', 'high', 'medium', 'low'] },
          category: { type: 'string' },
          rationale: { type: 'string', description: 'why this rank, tied to the fintech-competence goal' },
          scope: { type: 'string', description: 'concrete, surgical scope for the change' },
          effort: { type: 'string', enum: ['S', 'M', 'L'] },
          portfolio_value: { type: 'string', description: 'what hiring-manager-visible capability this proves' },
        },
      },
    },
    production_gaps: {
      type: 'array',
      description: 'What would be needed to actually operate this in production, grouped by theme.',
      items: {
        type: 'object',
        additionalProperties: false,
        required: ['theme', 'gap', 'severity'],
        properties: {
          theme: { type: 'string' },
          gap: { type: 'string' },
          severity: { type: 'string', enum: ['critical', 'high', 'medium', 'low'] },
        },
      },
    },
  },
}

// ---- Phase 1: parallel domain analysis -----------------------------------

const LENS = `You are analyzing a Java 25 + Spring Boot 4 fintech learning repo (a digital wallet domain) on the \`main\` branch. The owner's explicit goal is to PROVE fintech-backend competence (concurrency, data integrity, transactional outbox, SAGA, idempotency, observability) — treat that as the evaluation lens. Be a tough, senior fintech reviewer.

Rules:
- READ THE ACTUAL CODE. Cite file:line for every claim. Never speculate about code you did not open.
- Working dir is the repo root. Java under src/main/java/com/imwoo/airepo/wallet/{api,application,domain,infra,config}. Tests under src/test/java. Migrations under src/main/resources/db/migration. Frontend under frontend/.
- Distinguish genuine strengths from gaps. A learning repo that nails the hard parts is impressive; flag where it only *looks* thorough (doc/ADR theater) vs. where the code is actually robust.
- Prefer correctness/concurrency/data-integrity findings (the fintech core) over style nits. Be surgical in suggested fixes — no rewrites.`

phase('Analyze')

const ANALYSTS = [
  {
    key: 'concurrency-integrity',
    label: 'analyze:concurrency-integrity',
    prompt: `${LENS}

YOUR AREA: Concurrency & data integrity — the fintech core.
Investigate deeply:
- Balance mutation paths: charge/transfer. How is the row locked? (read src/main/resources/db/migration/V*.sql, the domain account/balance code, and the application services). Is there real row-level locking (SELECT ... FOR UPDATE / lock timeout) vs. only an H2 illusion? ADR 0011/0012 claim row locking — verify it in code, not just docs.
- Idempotency of charge/transfer (ADR 0006). Is the idempotency key actually enforced at the DB level (unique constraint) or only in app logic? Race window?
- Transactional outbox (ADR 0013-0020, 0027): is the outbox write in the SAME transaction as the balance mutation? Verify the @Transactional boundary in code.
- Money representation: BigDecimal vs double/long? Rounding? Negative-balance guard? Currency handling?
- SAGA / operation-step-log: is compensation real or just logged?
Return strengths AND findings per the schema.`,
  },
  {
    key: 'outbox-relay-consumer',
    label: 'analyze:outbox-relay-consumer',
    prompt: `${LENS}

YOUR AREA: Outbox relay → broker → consumer pipeline (the most-built part: ADR 0013-0054).
Investigate deeply:
- Relay scheduler & claiming: lease/recovery (ADR 0016/0017), guarded result update (ADR 0042), max-attempt → manual review (ADR 0018). Read the relay code and verify the claim-and-update is atomic (guarded UPDATE ... WHERE state=...). Any lost-update or double-publish window?
- Consumer idempotency & dedupe (ADR 0043/0044): processed-event table + unique constraint? At-least-once vs exactly-once reasoning correct?
- HTTP broker adapter (ADR 0035/0045/0046): is this a real broker or an in-process HTTP loopback? Be honest about whether this demonstrates real messaging competence or is a toy.
- Pruning jobs (0032/0048/0051/0053): correctness and safety (do they delete in-flight data?).
- Metrics/alerts (0033/0049/0050/0052/0054): are they wired to anything real, or recorded-and-forgotten?
Return strengths AND findings per the schema.`,
  },
  {
    key: 'security-authz',
    label: 'analyze:security-authz',
    prompt: `${LENS}

YOUR AREA: Security, authn/authz, admin API boundary.
Investigate deeply:
- SecurityConfig.java, AdminApiPathMatcher.java, AdminApiAccessAuditFilter — read them fully. The recent commits "harden admin api path matching" and "test-fixture admin boundary" suggest prior gaps. Are there path-matching bypasses (trailing slash, %2e, case, servlet path vs request URI)?
- Role model (ADR 0034) & operator/admin token split (ADR 0037): how are tokens validated? Hardcoded? Where do secrets live (.env.local, application*.yml)? Any secret committed?
- Is there real authentication (who is the user?) or only a static admin token? For a wallet, where is per-user authorization on balance/transfer endpoints — can user A move user B's money?
- TestFixtureController: can it be hit in prod profile? (the new disabled-test suggests this was a risk).
- Actuator exposure: which endpoints are public?
Return strengths AND findings per the schema.`,
  },
  {
    key: 'api-domain-architecture',
    label: 'analyze:api-domain-architecture',
    prompt: `${LENS}

YOUR AREA: API design, domain model, and hexagonal/layered architecture.
Investigate deeply:
- Layering: api → application → domain → infra. Are ports/adapters real (interfaces in application/domain, impls in infra) or is it leaky (controllers touching JDBC, domain importing Spring)? Check imports.
- Domain model richness: are Account/Wallet/Ledger rich domain objects or anemic structs? Where does business logic live?
- API design: REST resource modeling, error responses (RFC 7807 / problem+json?), validation (ADR 0023), pagination, versioning, status codes. Read the controllers.
- application package has 58 files — is that healthy decomposition or over-engineering / premature abstraction for a learning repo? Flag ADR/doc theater where structure exceeds substance.
- Consistency of the "operation" abstraction across charge/transfer/outbox.
Return strengths AND findings per the schema.`,
  },
  {
    key: 'observability-operability',
    label: 'analyze:observability-operability',
    prompt: `${LENS}

YOUR AREA: Observability & operability (real-operation readiness).
Investigate deeply:
- Logging: structured? correlation/trace id across the outbox pipeline? PII/secret leakage in logs? (ADR 0032 log pruning — read it).
- Metrics: Micrometer? actuator/prometheus? Are the "relay health metrics" (ADR 0033) actual Micrometer meters or just DB rows? Read the code.
- Health checks (ADR's actuator smoke, issue #89): what does /actuator/health actually check (DB, outbox lag)?
- Alerting (Slack webhook 0054): real integration or stub? Failure handling if Slack is down?
- Operational runbook / dashboards: do docs/ describe how an on-call would actually use this?
- Config & deployment: profiles (h2 vs postgres), 12-factor config, secrets, graceful shutdown, connection pool config. Is there ANY deployment artifact (Dockerfile, k8s, CI deploy)?
Return strengths AND findings per the schema.`,
  },
  {
    key: 'testing-ci',
    label: 'analyze:testing-ci',
    prompt: `${LENS}

YOUR AREA: Test strategy, quality, and CI.
Investigate deeply:
- 53 test files for 128 source files. Read a representative sample across api/application/domain. Are tests behavioral (assert real outcomes) or trivial (assert not null / mock-only)?
- Concurrency tests: is there ANY test that spawns concurrent charge/transfer and asserts no lost update / no negative balance? This is THE test a fintech reviewer looks for. Find it or confirm its absence.
- Scenario tests & postgres-scenario Testcontainers gate (ADR 0022/0036): do they exercise real flows end to end?
- Idempotency tests: replay-the-same-request tests?
- Frontend tests (vitest + playwright): coverage vs. the single App.tsx.
- CI: read .github/workflows/*. What actually runs on PR? Is the postgres-scenario / e2e gate enforced or optional? Coverage gates?
- Mutation/edge coverage: negative amounts, overflow, zero, concurrent retries.
Return strengths AND findings per the schema.`,
  },
  {
    key: 'frontend-product',
    label: 'analyze:frontend-product',
    prompt: `${LENS}

YOUR AREA: Frontend & end-to-end product completeness.
Investigate deeply:
- frontend/src/App.tsx and friends: is this a real usable wallet UI or a single-file demo? Read it.
- Does the product tell a coherent story end to end (user charges → transfers → sees history; operator reviews failed outbox)? Or is the backend depth disconnected from any usable surface?
- Auth on the frontend: how does it talk to the secured backend? Hardcoded token?
- The README is 26KB and there are 56 ADRs + 67 issue-drafts + wiki-drafts. Assess the doc-to-product ratio: is this a case of heavy documentation around a thin product? Be candid — this directly affects portfolio credibility.
- What is the single most impactful thing the FRONTEND/product layer needs to make the backend depth legible to a reviewer?
Return strengths AND findings per the schema. (Use 'frontend'/'docs'/'architecture' categories as fits.)`,
  },
]

const analyses = await parallel(
  ANALYSTS.map((a) => () =>
    agent(a.prompt, { label: a.label, phase: 'Analyze', schema: FINDINGS_SCHEMA, agentType: 'Explore' })
  )
)

const ok = analyses.filter(Boolean)
const allFindings = ok.flatMap((r) => (r.findings || []).map((f) => ({ ...f, area: r.area })))
const allStrengths = ok.flatMap((r) => (r.strengths || []).map((s) => ({ ...s, area: r.area })))
log(`Analyze done: ${allFindings.length} findings, ${allStrengths.length} strengths across ${ok.length} areas`)

// ---- Phase 2: adversarially verify high-severity findings ----------------

phase('Verify')

const toVerify = allFindings.filter((f) => f.severity === 'critical' || f.severity === 'high')
log(`Verifying ${toVerify.length} critical/high findings against the real code`)

const verified = await parallel(
  toVerify.map((f) => () =>
    agent(
      `You are an adversarial verifier. A prior analyst made this claim about the fintech wallet repo on \`main\`. Your job is to PROVE OR REFUTE it by reading the actual code — default to refuting if you cannot find concrete evidence.

CLAIM (severity=${f.severity}, category=${f.category}), area=${f.area}:
Title: ${f.title}
Evidence cited: ${f.evidence}
Why it matters: ${f.why_it_matters}

Open the cited files (and anything adjacent needed to judge). Quote the relevant lines. Decide if the issue is real, and what the correct severity is once you've seen the code. Be rigorous: many "findings" evaporate on inspection, and some are worse than claimed.`,
      { label: `verify:${f.area}:${f.title.slice(0, 40)}`, phase: 'Verify', schema: VERDICT_SCHEMA, agentType: 'Explore' }
    ).then((v) => ({ finding: f, verdict: v }))
  )
)

const confirmed = verified
  .filter(Boolean)
  .filter((v) => v.verdict && (v.verdict.verdict === 'confirmed' || v.verdict.verdict === 'partially-confirmed'))
  .map((v) => ({ ...v.finding, verified: v.verdict }))

// medium/low findings pass through unverified but clearly marked
const unverified = allFindings
  .filter((f) => f.severity === 'medium' || f.severity === 'low')
  .map((f) => ({ ...f, verified: { verdict: 'unverified', confidence: 'low', corrected_severity: f.severity } }))

const survivingFindings = [...confirmed, ...unverified]
log(`Verify done: ${confirmed.length}/${toVerify.length} high-sev findings confirmed; ${unverified.length} medium/low carried forward`)

// ---- Phase 3: synthesize prioritized backlog -----------------------------

phase('Synthesize')

const synthesis = await agent(
  `You are the lead diagnostician. Synthesize a final diagnosis of this fintech wallet repo (\`main\` branch) for an owner whose goal is to PROVE fintech-backend competence to hiring managers.

You are given:
1) STRENGTHS observed across areas (JSON).
2) FINDINGS that survived adversarial verification (high-sev ones are code-verified; medium/low are analyst-reported). Each has a \`verified\` block with corrected_severity.

De-duplicate findings that overlap across areas. Rank the backlog by (impact on proving fintech-backend competence) × (severity) ÷ (effort), but always float true correctness/concurrency/data-integrity bugs to the top. Give a candid seniority verdict — do not flatter. Identify production-readiness gaps grouped by theme.

STRENGTHS:
${JSON.stringify(allStrengths, null, 2)}

VERIFIED FINDINGS:
${JSON.stringify(survivingFindings.map((f) => ({ title: f.title, area: f.area, category: f.category, claimed_severity: f.severity, corrected_severity: f.verified?.corrected_severity, verdict: f.verified?.verdict, evidence: f.evidence, why: f.why_it_matters, fix: f.suggested_fix })), null, 2)}

Produce the synthesis per the schema.`,
  { label: 'synthesize:diagnosis', phase: 'Synthesize', schema: SYNTHESIS_SCHEMA }
)

return {
  counts: {
    areas: ok.length,
    findings_total: allFindings.length,
    findings_high_sev: toVerify.length,
    findings_confirmed: confirmed.length,
    strengths: allStrengths.length,
  },
  strengths: allStrengths,
  confirmed_findings: confirmed.map((f) => ({
    title: f.title,
    area: f.area,
    category: f.category,
    severity: f.verified?.corrected_severity || f.severity,
    verdict: f.verified?.verdict,
    evidence: f.evidence,
    why_it_matters: f.why_it_matters,
    suggested_fix: f.suggested_fix,
  })),
  medium_low_findings: unverified.map((f) => ({
    title: f.title, area: f.area, category: f.category, severity: f.severity,
    evidence: f.evidence, suggested_fix: f.suggested_fix,
  })),
  synthesis,
}
