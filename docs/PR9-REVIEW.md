# PR9 checkpoint — incomplete, do not merge

Branch: `feat/p1-ux-polish`.
Base: `ba7076fef8a2b70083ca434214a4c92d08ba0dc7` (merged PR8).
Exact checkpoint HEAD is reported in the Git handoff; this document cannot contain
its own commit hash. Recovered local implementation was uncommitted and is preserved.

## Done

- Five real workflow tracks, Step X of Y, persisted completion/failure handling,
  active-only animation/reduced motion, elapsed time and quiet polling. Manual
  Refresh now has independent loading state. No invented percentage or ETA.
- Summary/concern/candidate hierarchy, compact candidate grid, discoverable
  methodology/warnings and full-size evidence inspection. Authority is unchanged.
- Focusable frame/segment navigation, Home/End, visible playhead/source or clip time,
  I/O handles/range and Escape clear. Text inputs retain normal keys. No playback
  exists, so Space retains normal behavior. Marks are ephemeral inspection only.
- Actual analysis duration plus hash-matched packaged master provenance; historical
  PR8 master validation remains unchanged.
- Exact-name ordinary-project deletion, demo/active-run refusal, relational cascade,
  safe owned media cleanup and retry after cleanup failure. Neighboring/packaged
  files remain outside deletion. UUID tombstones prevent stale recreation.
- PR10 preparation inventory and ADR-0010; no broad PR10 implementation.

## Partial / not started

Active-frame scrolling currently runs for keyboard navigation; selection-driven
scrolling across every path remains partial. Visual review is representative, not
exhaustive final signoff. Latest requested Film Intelligence / Continuity Findings
tabs and clearer Import a shot / Continuity Findings purpose are not started.

Exact next task: implement the two-tab workspace hierarchy while retaining
Film Understanding polling, current selection/marks and guided-tour targeting.
Clarify the separate import and creator-confirmed continuity-review workflows.
Then centralize active-frame scroll and run focused tab/keyboard/tour/E2E checks.

## Verification evidence

- Full Gradle build: 127 backend tests, zero failures/errors/skips.
- Prior full frontend run: 96 passed. Fresh checkpoint run: 33 affected tests across
  seven files passed, including progress/polling, results, timeline, source and deletion.
- Fresh TypeScript/Vite production build and OpenAPI drift pass; generated deletion
  API contract included. Existing large entry warning now measures 531.50 kB.
- Isolated PR9 Chromium flow explicitly passed. Subsequent full run started 20 tests;
  recovered `test-results/.last-run.json` reports passed and no failed tests. Final
  console session was lost, so it is not presented as recovered console evidence.
- 21 screenshots under `test-results/pr9-{active,results,concern,evidence,playhead,range,delete}-{1280,820,390}.png`.
  Representative captures inspected; corrected modal captures wait for full opacity.
  Full visual review and latest requested tab hierarchy remain unfinished.
- Test API: loopback 8099, deterministic test-only ports, `sceneproof_pr9_browser`
  schema and `.local/pr9-browser-media`; web 5179. Existing user 8085/5173 services
  were left untouched. Recheck running processes before reusing these ports.

No real provider calls. No Analyze Selection, generation/Fix Assist, PR10 start or
merge. Browser screenshots contain explicitly test-only model results, never a
claimed live validation or production demo baseline.

## Reviewer focus and limits

Review V10 whole-project history deletion exceptions, cleanup recovery and trusted
local filesystem assumptions; backend tests cover full finding/action/reference
history as well as film state. DB/filesystem deletion is not one atomic transaction;
pending cleanup requires retry of the same UUID. Tiny tombstones are retained.
No public tenancy/auth claim. Sampled inspection has no playback, range persistence
or paid analysis semantics. PR10 security review is documented only.

Docs updated: STATUS, UX, PRODUCT, ARCHITECTURE, both plans, DEMO, DECISIONS,
intial-context (filename preserved), README, ADR index/ADR-0010 and PR10-SECURITY-PLAN.
This is a clean intermediate checkpoint, **not a merge-ready PR9 completion**.
