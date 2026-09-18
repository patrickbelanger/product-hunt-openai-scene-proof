# Development Plan

## Current slice — September 17, consolidated launch recovery

Consolidate the preserved continuity CTA and privacy/terms disclosures into the
existing `bugfix/continuity-analysis-cta`. Keep the privacy branch/worktree intact;
create no branch/worktree, push nothing and do not merge main. Complete combined
deterministic verification and local commits. Disclosure operator TODOs and public
exposure review remain launch gates; September 18 scope/date are unchanged.

## Previous slice — September 15, post-PR10

PR10 is merged at `826bf99`. The authorized release-blocker correction is
`bugfix/continuity-analysis-cta`: expose the existing continuity start operation,
preserve request replay/security/provider semantics and verify regressions without
live provider calls. September 18 scope/date are unchanged. Earlier roadmap and
handoff sections below are historical. Stop after implementation and verification.

## Current roadmap — PR9 authorization

PR8 AI Film Understanding / Multimodal Continuity Discovery is COMPLETE and merged
at `ba7076fef8a2b70083ca434214a4c92d08ba0dc7`. PR9 UX Polish + Project Lifecycle
Usability is CURRENT on `feat/p1-ux-polish`. PR10 Security Hardening is NEXT (HIGH
reasoning), with [preparation inventory](PR10-SECURITY-PLAN.md) only in PR9.
Selected-range Film Understanding/Analyze Selection follows PR10 unless separately
reprioritized. September 18 is unchanged. Earlier handoff/ordering statements below
are historical and superseded by this approved sequence; no standalone progress PR.
PR9's final workspace tab/inspection slice is implemented and verified; final review
handoff is in STATUS and PR9-REVIEW. PR10 is not started by this work.

## Current handoff — September 14

PR7 is merged. PR8 is implemented and ready for merge review, with successful
master-source provider processing and separately verified derived-source generation.
No live derivative success is claimed. [Final evidence](PR8-FINAL-VALIDATION.md)
records remaining limits and PR9-only UX issues. Next: PR9 `feat/p1-ux-polish`, only
after approval; optional P2 follows. No standalone analysis-progress branch exists.

Historical delivery baseline: September 12, 2026. The initial schedule below is
preserved; Patrick's authorized post-PR7 branch/scope reconciliation follows it.
Deadline: September 18; this planning update does not revise the delivery date.

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
`feat/p1-guided-tour`, `feat/p1-demo-project`, `feat/p1-ai-film-understanding`,
`feat/p1-ux-polish`, optional `feat/p2-product-hunt-polish`.

The earlier plan placed standalone `feat/p1-analysis-progress` immediately after
PR7. Patrick supersedes that next slice with **PR8 — AI Film Understanding /
Multimodal Continuity Discovery** on `feat/p1-ai-film-understanding`, only after
PR7 is merged (verified `ed6066283d27da97484425f900b6bccb447b0911`). PR8 absorbs the essential truthful progress needed by that vertical
slice: durable backend processing stages, real UI stage visibility, reload/recovery
of an active run and pipeline failure visibility. Backend state is authoritative;
no fake percentages or invented progress. ADR-0008 now defines persisted stages and
1.5-second active-run GET polling, not SSE; process-loss recovery never replays paid work.

Full-film understanding combines audio/transcript and visual context to propose
candidate Reference Bible entries/continuity anchors for creator confirmation.
Generalized retry UX, unrelated failure polish, public hosting/anonymous isolation,
production spend/rate protection and broader responsive/accessibility/launch polish
remain separate subsequent hardening unless required by PR8-touched surfaces.
The earlier ADR-0007 suggestion that PR8 could bind public sessions is a future
architectural possibility, not authorization to include public isolation in PR8.
The standalone progress branch is no longer a separate next delivery slice; later
`feat/p1-ux-polish` and optional Product Hunt polish remain in the plan.

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
PR7 is merged through PR #7 at `ed6066283d27da97484425f900b6bccb447b0911`. Patrick accepts its honest
zero-finding validation as evidence of independent analysis, not accuracy or a
finding showcase. This documentation-only reconciliation does not reopen PR7 work
or authorize another PR7 provider call. Patrick now authorizes PR8 implementation
from that exact merged base, including at most one real transcription and one real
Film Understanding call after deterministic verification, with no reroll.
The authoritative forward path is PR7 merged → PR8 Film Understanding and required
truthful stages → PR9 `feat/p1-ux-polish` → optional P2 Product Hunt polish.
The dates are delivery targets, not assertions of completed work; actual progress
lives in [STATUS](STATUS.md) and [Implementation Plan](IMPLEMENTATION-PLAN.md).

## Risk buffer and launch dependencies

Select hosting, storage persistence, anonymous isolation and concrete paid-demo
quota by Day 3. These material decisions require Patrick's confirmation. Defer
optional animations, additional finding categories in the demo and elaborate
settings first if the critical path slips. Do not replace real Astra with fake data.
