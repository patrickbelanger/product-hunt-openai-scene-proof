# Development Plan

Baseline: September 12, 2026. Implements the priorities and branch order authorized
in the initial context; no change to product scope. Deadline: September 18.

| Day | Goal | Exit evidence |
| --- | --- | --- |
| Sep 12 — Day 1 | P0 foundation; start media ingestion after review | Runnable API/web, real PostgreSQL project flow, green checks |
| Sep 13 — Day 2 | P0 media, Astra and findings | Video → bounded frames → live Astra → persisted finding → visual inspection |
| Sep 14 — Day 3 | P0 intentional-change steering; Reference Bible | Context persisted, affected analysis repeated, auditable updated finding |
| Sep 15 — Day 4 | P1 demo, guided tour, real progress | Curated film and independent continuity evaluation; bounded live use |
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
PR2 is merged into main as `602cf1c`, based on `7275f17`;
real transport and imported-project persistence smoke tests passed.
Finding persistence/API move into PR2 under Patrick's explicit scope; PR3 owns UI.
PR3 is merged through PR #3 as `a821d89`, including the review handoff `3312550`.
Patrick authorized PR4 on `feat/p0-intentional-change-steering`: immutable creator
context, independent targeted judgement and durable resolve/dismiss. PR4 is merged
through PR #4 as `0901f4b`; Patrick authorizes PR5 Reference Bible from that base.
PR5 is merged through PR #5 as `3184af6`; PR6 recovery verified the exact main SHA
`3184af6cf0119be1fa2949ca1fb60a2584483a56`. Patrick authorizes only
`feat/p1-guided-tour`: optional four-step onboarding, no model/backend changes.
PR6 is merged through PR #6 at `1e3f28fb162bcdc9d608c40d61d7fcfbcad97909`.
Patrick authorizes PR7 on `feat/p1-demo-project`: approved original film, up to eight
clips, five references, deterministic creation/reset, evaluation separation and
honest empty baseline. Current curation supersedes the unsupported blazer candidate.
No analysis-limit change, public isolation/deployment or later scene-management work.
PR7 verification passes: 83 backend, 68 frontend and 17 Chromium tests. The one real
film analysis succeeded with zero findings and contextualized the apartment transition;
the historical ~five-issue target is not a promise of detections. A finding-focused
launch recording remains a curation/review question, not grounds for a paid reroll.
Commit/push PR7, then stop for review; do not start PR8. No dates or
priorities have been changed. The dates are delivery targets, not assertions of completed work; actual progress
lives in [STATUS](STATUS.md) and [Implementation Plan](IMPLEMENTATION-PLAN.md).

## Risk buffer and launch dependencies

Select hosting, storage persistence, anonymous isolation and concrete paid-demo
quota by Day 3. These material decisions require Patrick's confirmation. Defer
optional animations, additional finding categories in the demo and elaborate
settings first if the critical path slips. Do not replace real Astra with fake data.
