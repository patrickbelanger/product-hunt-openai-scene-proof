# Implementation Plan

## P0 foundation — complete, ready for review on `feat/p0-foundation`

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
contract during real HTTP checks. Foundation is not yet integrated into main.

Foundation acceptance: create a project with rules in React, store in PostgreSQL,
open its workspace, reload and retrieve the same project. Invalid names produce
actionable errors. Empty media/findings are not fabricated. No API key required.

## First complete continuity vertical slice

1. `feat/p0-media-ingestion`: MediaStorage and local adapter; image validation,
   bounded upload; controlled FFprobe/FFmpeg adapter; representative sampling,
   shots/frames migration, order and timestamp invariants; hostile input tests.
2. `feat/p0-astra-analysis`: small original fixture; ContinuityAnalysisPort;
   verify current API/transport; bounded reference+neighbor request; JSON Schema;
   validate IDs, evidence and confidence; AnalysisRun lifecycle; real call and
   persisted usage/failures; refusal/incomplete/malformed response tests.
3. `feat/p0-findings-workspace`: finding persistence/API/client; timeline markers,
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
