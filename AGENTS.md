# SceneProof engineering protocol

Read `docs/STATUS.md`, `README.md`, `docs/BRD.md`, `docs/DEVELOPMENT-PLAN.md`,
`docs/IMPLEMENTATION-PLAN.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md` and relevant
ADRs before changing code. Inspect the actual branch, worktree and recent commits.
Reconcile discrepancies; never re-scaffold valid existing components.

The detailed recovery and handoff protocol is in `docs/intial-context.md`, sections
34–44. Preserve that original context as historical input; accepted clarifications
are recorded in `docs/DECISIONS.md` and ADRs.

Use short-lived `feat/`, `fix/`, `docs/`, `chore/`, `refactor/`, `hotfix/` branches
with lowercase kebab-case names. No substantial feature development on main.
Create only the branch for the active slice. Do not merge without review.

Keep Kotlin / Spring Boot / Java 25, React 19 / Mantine, PostgreSQL and Gradle.
No code or proprietary concepts from Cyrantis, PGS or lxp-llm-gateway.
No fabricated model results in runtime paths. Keep secrets out of source and logs.

Material product, architecture, security, cost or deadline decisions require
`DECISION REQUIRED`: current choice, problem, proposed alternative, alternatives,
recommendation, implementation impact and September 18 delivery impact. Continue
independent work while a decision is pending. Resolve ordinary details autonomously.

Before handoff: relevant tests and builds, synchronized technical docs, ADRs for
significant decisions, current `STATUS.md` and implementation checkboxes. Distinguish
implemented, verified, planned and blocked behavior. Never claim skipped checks pass.
