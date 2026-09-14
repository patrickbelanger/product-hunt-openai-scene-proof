# Implementation Plan

## P0 foundation — merged into main as `fefb7db` (PR0)

- [x] Recover actual repository state; preserve existing work.
- [x] Establish BRD, delivery plan, technical plan and decision records.
- [x] Confirm Boot 4.1.1 interpretation with Patrick; inspect Java/Node/tooling.
- [x] Gradle wrapper with distribution checksum, Java 25 / Kotlin backend and pinned frontend workspace.
- [x] PostgreSQL Compose, Flyway migration and Project persistence.
- [x] Validated project REST API, stable errors, OpenAPI and generated TS types.
- [x] Landing, project creation and visual workspace with truthful empty states.
- [x] Backend database/API tests and frontend critical path tests.
- [x] Reproducible startup, CI workflow, builds and browser smoke verification (remote CI awaits push).
- [x] Synchronize final status and architecture; prepare review handoff.

Verification: 4 backend + 4 frontend + 2 live API/browser tests pass; builds and
OpenAPI drift check pass; clean npm install and zero-vulnerability audit. Found and
fixed PostgreSQL timestamp precision drift and corrected the RFC 9457 default type
contract during real HTTP checks. PR0 integration was verified during PR1 recovery.

Foundation acceptance: create a project with rules in React, store in PostgreSQL,
open its workspace, reload and retrieve the same project. Invalid names produce
actionable errors. Empty media/findings are not fabricated. No API key required.

## First complete continuity vertical slice

### PR1 — merged into local main from `feat/p0-media-ingestion` (`95ee2f9`)

- [x] Verify clean worktree, fetch origin, fast-forward main to PR0 merge and branch.
- [x] MediaStorage/local adapter, generated keys, bounded streaming upload.
- [x] JPEG/PNG signature, dimension and decode validation; normalized frame content.
- [x] Controlled FFprobe/FFmpeg, H.264 metadata bounds, scene/fallback extraction.
- [x] Shot/frame migration, atomic metadata writes, ordering and timestamp invariants.
- [x] Safe persisted failed attempts and normal failure cleanup.
- [x] Typed multipart API, upload UI, actual frame selection and reload.
- [x] PostgreSQL/media integration tests and frontend upload/retry tests.
- [x] Backend build (13 tests after compatibility correction), frontend build and OpenAPI drift check.
- [x] Final Chromium flow and desktop/tablet visual verification (3 E2E tests).
- [x] Synchronized handoff and ADR-0003.
- [x] Patrick approved PR1; integrate `95ee2f9` into local main after final green checks.
- [x] Verify targeted MP4 major-brand compatibility correction: real iso6 MP4 accepted,
  corrupt ftyp-only input rejected, existing codec/duration tests retained.

No Astra, findings implementation, Reference Bible CRUD, public hosting or paid
calls are part of PR1. Abrupt termination orphan cleanup is documented local debt.

### PR2 — merged into main as `602cf1c`, base `7275f17`

- [x] Recovery, exact main SHA, PR1 code inspection and branch creation.
- [x] Re-verify official Astra/Responses/image/schema/SDK/error documentation.
- [x] Real original-image transport spike, authentication, parsing and usage.
- [x] Port/JDK HTTP adapter, bounded context, strict schema and scope validation.
- [x] V3 AnalysisRun/Finding/evidence persistence, deduplication and failure states.
- [x] Analysis/findings endpoints, OpenAPI and generated TypeScript client.
- [x] Complete backend (34), frontend (6), builds, Chromium (4) and contract checks.
- [x] Real imported-project analysis with a persisted finding and usage; verify after restart.
- [x] Final review handoff and verified status; subsequently merged through PR #2.

### PR3 — merged as `a821d89`, `feat/p0-findings-workspace`, base `602cf1c`

- [x] Recover clean synchronized main at the requested SHA; reconcile PR2 merge documentation.
- [x] Consume generated findings client with honest loading/error/empty and URL selection state.
- [x] Findings detail, affected-shot timeline markers and navigation.
- [x] Deterministic real evidence comparison, additional frames and missing-image handling.
- [x] Expected/observed/explanation/confidence/severity and real clipboard feedback.
- [x] Targeted frontend tests and critical Chromium persisted-findings flow, with no paid calls.
- [x] Backend/frontend builds, tests, OpenAPI drift and diff checks; visual laptop/tablet QA.
- [x] Synchronize documentation and stop for review without merge or steering.

Verification: 34 backend, 21 frontend and 5 Chromium tests pass. Normal suite uses
the test-only port plus real persistence. The original PR2 live finding was also
inspected without any new provider call. Exact evidence and limits: PR3-REVIEW.
Implementation commit `fa0dbc9` is pushed; the review-preparation documentation
follow-up preserves the same scope and base. PR #3 subsequently merged `3312550`
into main at `a821d89`; PR4 recovery verified the exact synchronized SHA.

Patrick explicitly includes finding persistence/read API in PR2. This supersedes
the older placement below; PR3 delivered the findings workspace UI.

1. `feat/p0-media-ingestion`: MediaStorage and local adapter; image validation,
   bounded upload; controlled FFprobe/FFmpeg adapter; representative sampling,
   shots/frames migration, order and timestamp invariants; hostile input tests.
2. `feat/p0-astra-analysis`: small original fixture; ContinuityAnalysisPort;
   verify current API/transport; bounded reference+neighbor request; JSON Schema;
   validate IDs, evidence and confidence; AnalysisRun lifecycle; real call and
   persisted usage/failures; refusal/incomplete/malformed response tests.
3. `feat/p0-findings-workspace`: consume PR2 findings API/client; timeline markers,
   findings panel, affected-shot selection, two-frame comparison, expected versus
   observed, explanation and copy correction; critical browser test.

Each branch adds migrations and updates DOMAIN, ARCHITECTURE, UX, relevant media/AI
docs, plans and STATUS in the same pass. Complete the real flow before visual polish.

## Second complete vertical slice

`feat/p0-intentional-change-steering`: explanation and affected scope, immutable
context history, targeted re-analysis, finding supersession, resolve/dismiss actions,
failure-safe UI refresh. Test state transitions and preservation of prior evidence.

### PR4 — merged as `0901f4b`, implementation on base `a821d89`

- [x] Complete recovery, reconcile PR3 merge and create only the authorized branch.
- [x] V4 immutable actions/scope/results with strong original-finding/run ownership.
- [x] Reuse durable run admission, request replay, usage and failure recovery.
- [x] Typed targeted port operation, original evidence plus bounded immediate neighbors.
- [x] Strict independent judgement: INTENT_ACCEPTED / ISSUE_REMAINS / INSUFFICIENT_EVIDENCE.
- [x] Atomic effective-judgement supersession; preserve all original fields/evidence.
- [x] Creator resolve/dismiss without inference; reject invalid state transitions.
- [x] OpenAPI actions/history contract and regenerated TypeScript client.
- [x] Compact intent form, explicit confirmation, pending/failure/recovery and history.
- [x] Final backend/frontend/build/OpenAPI/diff and Chromium verification: 52 / 31 / 8 tests.
- [x] One successful original-fixture targeted Astra smoke and restart verification; real ISSUE_REMAINS, no forced acceptance.
- [x] Synchronize PR4 review handoff and stop without merge or starting P1.

## P1 / P2

### PR5 — Reference Bible, base `0901f4b`, merged as `3184af6`

- [x] Recover exact main, reconcile PR4 merge, create only PR5 branch.
- [x] V5 references, immutable visual evidence, metadata snapshots and archive lifecycle.
- [x] Rules update and secure JPEG/PNG reference APIs, generated OpenAPI client.
- [x] Bounded sequence references, citations, persistence and historical targeted context.
- [x] Reference Bible editor and historical finding reference evidence.
- [x] Deterministic backend/frontend/Chromium tests, builds and contract checks: 71 / 44 / 10.
- [x] One paid original-fixture smoke after deterministic checks; real citation, replay and no-key restart verified.
- [x] Synchronize docs, ADR-0006 and PR5-REVIEW; stop for review without merge.

- [x] Reference Bible editor, rules and reference-image association (PR5).
- [x] Optional four-step tour with Skip and Restart (PR6; verified below).
- [x] Original demo media, evidence-based evaluation questions, intentional transition and reset (PR7; no guaranteed finding count).
- [ ] Bounded live demo, hosting/isolation decision and rate/cost protection.
- [ ] Generalized retry UX and unrelated failure-state polish; essential Film Understanding stage visibility/recovery moves to PR8 below.
- [ ] Responsive/accessibility polish outside PR8-touched surfaces and launch rehearsal.
- [ ] Optional micro-interactions and Product Hunt presentation refinements.

### PR6 — Guided tour, base `3184af6cf0119be1fa2949ca1fb60a2584483a56`, merged as `1e3f28f`

- [x] Recover clean main/origin at the PR5 merge; reconcile historical PR5 review state.
- [x] Create only `feat/p1-guided-tour` from the exact verified base.
- [x] Optional invitation, four concise steps, Skip/Done and persistent Quick tour restart.
- [x] Versioned localStorage preference with safe failure behavior; no backend state.
- [x] Stable semantic panel anchors, empty findings and truthful hidden-target fallback.
- [x] Mantine dialog focus, keyboard/Escape, reduced motion and viewport-bounded card.
- [x] Deterministic RTL coverage: 15 tour tests; complete frontend suite 59 passing.
- [x] Final Chromium desktop/tablet/mobile and existing regression checks: 59 frontend / 15 browser / 71 backend; builds, OpenAPI drift and diff pass.
- [x] Synchronize final verification/review docs for the PR6 commit and push; stop without merge or PR7.

### PR7 — Populated demo, exact base `1e3f28fb162bcdc9d608c40d61d7fcfbcad97909`

- [x] Recover exact merged PR6 main; preserve supplied staged film; create only PR7 branch.
- [x] Confirm Patrick's original/generated source authorization and Laurie/Patrick attribution.
- [x] Inspect actual film; approve up to eight clips and supersede unsupported blazer assumption.
- [x] Versioned runtime input manifest, eight clips, five derived references and provenance/curation record.
- [x] Evaluation-only continuity questions and historical unsupported candidates outside runtime resources.
- [x] V6 explicit demo identity, atomic seed/recovery, safe replacement reset with retained history.
- [x] Typed API/OpenAPI regeneration, landing entry, demo indicator and confirmed reset.
- [x] Backend/frontend deterministic tests and builds: 83 backend / 68 frontend; OpenAPI drift.
- [x] Complete Chromium (17 tests) and 1280/820/390 visual, keyboard and no-overflow verification.
- [x] Exactly one real full-sequence Astra validation: succeeded, zero findings; independent results/usage in PR7-REVIEW. No reroll.
- [x] Final synchronized PR7 review; implementation `5eac0b9` committed/pushed. Documentation-only handoff follow-up; stop without merge or PR8.

### PR8 — AI Film Understanding / Multimodal Continuity Discovery (review handoff)

Active branch: `feat/p1-ai-film-understanding`, exact base
`ed6066283d27da97484425f900b6bccb447b0911` (verified PR7 merge).
The historical/superseded standalone `feat/p1-analysis-progress` is absorbed into
PR8; there is no separate progress branch. General UX remains PR9.

- [x] Recover merged PR7 and create only the authorized PR8 branch.
- [x] Reconcile initial context priorities/branch plan/merge order with Patrick's explicit decision.
- [x] SourceFilm, bounded upload and deterministic segments/frame provenance.
- [x] FFmpeg audio extraction and verified AudioTranscriptionPort implementation.
- [x] FilmUnderstandingPort, Astra MEDIUM strict schema and ID/evidence validation.
- [x] Full-film understanding using audio/transcript plus visual context.
- [x] Propose candidate Reference Bible entries and continuity anchors grounded in the film.
- [x] Creator confirmation of candidates before adopting them as declared project context.
- [x] Accept/Edit/Reject, confirmed memory and normal ReferenceService visual promotion.
- [x] Later continuity context integration and transcript/narrative finding evidence.
- [x] Explicit paid consent, durable deduplication, bounded cost and safe new attempts.
- [x] Persist truthful backend processing stages and expose authoritative stage state in the UI.
- [x] Reload/recover an active Film Understanding run and expose its actual failures; no fake percentages or invented progress.
- [x] Between the Line source integration, isolated reset/history and evaluation exclusion.
- [x] Deterministic backend/frontend tests, Chromium, builds and generated OpenAPI checks: 102 / 78 / 19.
- [x] Capped real validation: one transcription request rejected; zero Astra calls. Failure/unknown cost recorded, no reroll.
- [ ] Successful real multimodal provider acceptance remains unverified; a new attempt requires Patrick's separate authorization after diagnosing the rejection.
- [x] Synchronized product/technical docs, ADR and PR8-REVIEW for the final commit/push handoff; stop for review.

Implemented stages (ADR-0008): PREPARING_SOURCE, DETECTING_STRUCTURE,
TRANSCRIBING_AUDIO, UNDERSTANDING_FILM, BUILDING_CANDIDATES, SUCCEEDED, FAILED.
V7 persists actual stage transitions; frontend active-state polling is authoritative,
not SSE or synthetic progress. V8 protects decisions/results and reference provenance.

Residual generalized retries, unrelated failure polish, public hosting/anonymous
isolation, production spend/rate protection and final launch hardening remain separate.
Later `feat/p1-ux-polish` and optional `feat/p2-product-hunt-polish` remain planned;
PR9 owns general UX polish; optional P2 must not delay September 18. Do not start PR9 here.
