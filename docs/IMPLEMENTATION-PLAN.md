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

### PR3 — ready for review `feat/p0-findings-workspace`, base `602cf1c`

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

Patrick explicitly includes finding persistence/read API in PR2. This supersedes
the older placement below; the next branch is solely the findings workspace UI.

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

## P1 / P2

- [ ] Reference Bible editor, rules and reference-image association.
- [ ] Optional four-step tour with Skip and Restart.
- [ ] Original demo media, five expected issues, intentional differences and reset.
- [ ] Bounded live demo, hosting/isolation decision and rate/cost protection.
- [ ] Real progress transport, retries and failure visibility.
- [ ] Responsive/accessibility polish and launch rehearsal.
- [ ] Optional micro-interactions and Product Hunt presentation refinements.
