# Development Plan

Baseline: September 12, 2026. Implements the priorities and branch order authorized
in the initial context; no change to product scope. Deadline: September 18.

| Day | Goal | Exit evidence |
| --- | --- | --- |
| Sep 12 — Day 1 | P0 foundation; start media ingestion after review | Runnable API/web, real PostgreSQL project flow, green checks |
| Sep 13 — Day 2 | P0 media, Astra and findings | Video → bounded frames → live Astra → persisted finding → visual inspection |
| Sep 14 — Day 3 | P0 intentional-change steering; Reference Bible | Context persisted, affected analysis repeated, auditable updated finding |
| Sep 15 — Day 4 | P1 demo, guided tour, real progress | Curated film with ~5 issues and intentional differences; bounded live use |
| Sep 16 — Day 5 | P1 polish, deployment rehearsal, recording | Laptop/tablet QA; launch candidate and recording rehearsal |
| Sep 17 | Buffer and demo freeze | Fix launch blockers only, record 60–75s demo, prepare submission |
| Sep 18 | Submit and monitor | Public URL, screenshots, video, tested first-run experience |

Critical path: foundation → media → Astra structured output → findings inspection
→ intentional-change re-analysis → bounded public demo. A minimal original fixture
is needed during P0; polished demo packaging remains P1. Reference CRUD must not
delay the first model call. The first live call should establish access and cost
early on Day 2. No P2 animations or launch cosmetics may delay P0.

## Review and branches

Use, in order: `feat/p0-foundation`, `feat/p0-media-ingestion`,
`feat/p0-astra-analysis`, `feat/p0-findings-workspace`,
`feat/p0-intentional-change-steering`, `feat/p1-reference-bible`,
`feat/p1-guided-tour`, `feat/p1-demo-project`, `feat/p1-analysis-progress`,
`feat/p1-ux-polish`, optional `feat/p2-product-hunt-polish`.

Create each branch when work begins, after prerequisites are integrated into main.
Keep main runnable, no permanent develop branch. Feature documentation travels
with implementation. Review requires tests, builds, current status and known limits.
September 12 checkpoint: PR0 foundation is merged on main (`fefb7db`). PR1 media
ingestion (`95ee2f9`) is approved, locally verified and merged into local main.
PR2 has not started; this session stops after merge verification as requested.
No dates or
priorities have been changed. The dates are delivery targets, not assertions of completed work; actual progress
lives in [STATUS](STATUS.md) and [Implementation Plan](IMPLEMENTATION-PLAN.md).

## Risk buffer and launch dependencies

Select hosting, storage persistence, anonymous isolation and concrete paid-demo
quota by Day 3. These material decisions require Patrick's confirmation. Defer
optional animations, additional finding categories in the demo and elaborate
settings first if the critical path slips. Do not replace real Astra with fake data.
