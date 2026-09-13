# PR3 review handoff — 2026-09-12

Present-state correction during PR4 recovery: PR3 is merged through GitHub PR #3
at `a821d89c4f7aa0396cf447ec9951f189d42617f2` (parents `602cf1c`, `3312550`).
The pending-review/merge statements in this document preserve the historical PR3
handoff. PR4 is separately authorized on `feat/p0-intentional-change-steering`.

## Branch and recovery

- Branch: `feat/p0-findings-workspace`.
- Base: `602cf1c2a1dac4c6dcd563d01a3e580c53caef59`.
- Committed and pushed implementation: `fa0dbc9cb25e56612bb5d7b4891ae272f4f2179f`.
  Review preparation verified this SHA on GitHub and a clean worktree. This
  documentation-only follow-up corrects the former uncommitted/unpushed state.
- Follow-up push authentication resolved: the initial wrong-account HTTP 403 was
  corrected by selecting `patrickbelanger`, Patrick's required GitHub account.
  Review follow-up `1cdfe85` was successfully pushed to this branch; this final
  documentation correction records the resolution. No force push or merge.
- Branch: https://github.com/patrickbelanger/product-hunt-openai-scene-proof/tree/feat/p0-findings-workspace
- External code review pending. No merge or steering branch/implementation.
- Initial worktree was clean on PR2 `fe89f16`; local main was stale at `7275f17`.
  Fetch confirmed PR #2 merge `602cf1c`. Main was fast-forwarded and verified clean,
  identical to origin/main, before creating only the requested PR3 branch.
- Read recovery sections 34–44, required product/technical documents, all existing
  ADRs including ADR-0004, and the actual frontend/client/PR2 persistence and tests.
  Reconciled pending-merge handoff text with Git evidence. Original context preserved.

## Implemented behavior

The existing Reference Bible | Viewer / Evidence | Findings composition is retained.
FindingsWorkspace reads the generated PR2 client with TanStack Query, twenty entries
per page. Selection scopes the URL to the finding's analysis, preserving navigation
on reload even if newer analyses arrive. All saved findings returns to history.
Linked analysis IDs expose durable status, including failed or empty successful runs.

Category, textual severity, model-reported confidence, title, summary, expected and
observed states, explanation and full correction are visible. Copy waits for the
actual clipboard promise and announces success/failure; text supports manual copy.
Buttons support keyboard activation and visible focus. View evidence comparison
focuses the viewer heading, particularly useful in the tablet layout.

Page-scoped timeline markers and selected-finding highlights connect affected shots.
Navigation scrolls/focuses the real evidence frame. Comparison joins persisted
relevantFrameIds to affected Shot/Frame IDs, sorted by shot position, frame position
and UUID. First frame stays left; the first frame from a different shot is preferred
right, otherwise the second from the same shot. Extra evidence is accessible through
right-side selection buttons. One frame remains useful; missing metadata and image
load failures are explicit. No image crop, recoloring or proof-obscuring overlays.

Import collapses during finding inspection. Other timeline frames remain inspectable,
with a return-to-evidence action. Loading/error/empty states do not fabricate results.
No React analysis mutation exists: load, finding/frame/shot selection, refresh and
reload use saved data only. No Astra adapter, migration or API contract changed.

## Main files

- `apps/web/src/projects/FindingsWorkspace.tsx`: data/URL coordination and finding detail.
- `apps/web/src/projects/EvidenceComparison.tsx`: evidence mapping, pair and image errors.
- `apps/web/src/projects/MediaWorkspace.tsx`, `Workspace.tsx`, `styles.css`: integration,
  timeline, responsive layout and visual treatment.
- `apps/web/src/projects/findings.test.tsx`: 15 new focused tests; existing project
  tests updated for the honest new empty state.
- `tests/e2e/findings.spec.ts`: actual imported pixels → test analysis → persisted
  findings → UI comparison → clipboard/reload/responsive, plus OpenAPI validation.
- `apps/api/src/test/kotlin/dev/sceneproofbrowser/FindingsBrowserApplication.kt`,
  `application-browser.yml`, API Gradle task and CI: deterministic provider only on
  test classpath; real services, isolated schema/media; guarded E2E fixture creation.
- Vite proxies the test marker for E2E; production jar contains no marker or fixture.
  Local media ignores cover module-relative directories as well as repository root.

## Verification

| Check | Result |
| --- | --- |
| Gradle build / backend tests | PASS, 34 tests, 0 failures, 0 skipped |
| Vitest / RTL | PASS, 21 tests total (15 findings + 6 existing) |
| Chromium suite | PASS, 5 tests, real API and PostgreSQL/media |
| TypeScript / Vite production build | PASS |
| OpenAPI generated-type drift | PASS, contract and generated DTOs unchanged |
| git diff --check | PASS |
| Remote CI | Status unverified: GitHub CLI returned HTTP 401 Bad credentials |

The final Gradle invocation reused the already successful unchanged test outputs.
Review preparation reran all 21 frontend tests, TypeScript/Vite build and OpenAPI
drift successfully. Backend and Chromium passing results above belong to the
unchanged implementation; they were not rerun for the documentation-only follow-up.
The earlier first TypeScript pass, initial overly-specific accessible-name assertion,
duplicate test-marker registration, sandbox browser launch and old empty-state E2E
assertion failed before correction. Those attempts are not counted as passing checks.

Frontend tests cover loading, empty vs successful empty, failure and retry, unknown
finding links, URL/page/run restoration, affected-shot navigation/highlighting,
one/two/multiple/missing frames, same-shot video timestamps, image errors, expected /
observed / explanation / confidence / prompt, keyboard clipboard pending/success /
failure/retry, feedback reset, history pagination, running/failed/unavailable analyses
and shots API failures. Tests assert that display/refresh never creates analysis.

The Chromium critical path creates three original images and uses the test-only
ContinuityAnalysisPort with real PR2 validation/persistence; it checks returned
Finding against OpenAPI, actual loaded pixels, selected shots, comparison switching,
clipboard contents, denied clipboard feedback, reload and no UI analysis POSTs.
No paid call is part of normal tests or this PR3 session.

## Real PR2 read-only verification and screenshots

The original PR2 project `dc4ee12a-b72d-43a3-a072-9a7fe091ccdf` remains persisted.
Run `ce4d940d-a3c0-4f26-88e2-44a108e9767c`, finding
`9e9fd139-241f-4136-a931-61d5d4c0b613`, was inspected against the normal production
jar on 8090 with no provider key. Two actual evidence PNGs loaded, the clipboard
matched its persisted correction, reload restored selection and zero browser writes
were observed (non-GET API requests were actively blocked in this separate check).

Inspected local captures:

- `test-results/findings-desktop.png` / `findings-tablet.png`: deterministic persisted
  browser fixture; 1280px desktop / 820px tablet.
- `test-results/findings-real-pr2-desktop.png` / `findings-real-pr2-tablet.png`: actual
  PR2 provider result; 1440px desktop / 820px tablet.
- Chromium additionally checks 390px mobile overflow. No horizontal overflow found.

Screenshots and the local read-only smoke script are ignored verification artifacts,
not production assets or demo data. The synthetic squares prove data linkage and
inspection behavior; they are not a film-continuity accuracy benchmark.

## Known limits and review

Analysis initiation remains explicitly through the existing API. PR2 provides no
latest-run/list-runs endpoint; an unfiltered project view cannot discover a failed
run ID. Link `?analysisId=<run UUID>` to inspect its status. An empty unfiltered list
never claims success. Historical findings remain OPEN under PR2 semantics, with no
supersession, steering or resolution work added. Markers cover only the current page.
Alternate comparison frames are temporary; reload returns to the deterministic pair.
No Reference Bible editor, guided tour, curated demo, hosting or P1/P2 polish.

During implementation verification: Vite `http://127.0.0.1:15177` → API 8090 (PID 150108),
no provider key. The previous 8088 server was not running. Existing IDE processes
were preserved. Tests use a separate schema; named verification projects remain.

Updated docs: README, STATUS, UX, ARCHITECTURE, IMPLEMENTATION-PLAN,
DEVELOPMENT-PLAN and the historical PR2-REVIEW banner; this PR3 review added.
No new material architecture decision; ADR-0004 remains authoritative and unchanged.
September 18 target and branch order unchanged. Stop for review; do not merge or
start `feat/p0-intentional-change-steering`.
